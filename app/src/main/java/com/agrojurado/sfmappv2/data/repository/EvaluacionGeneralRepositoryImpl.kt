package com.agrojurado.sfmappv2.data.repository

import android.content.Context
import android.util.Log
import com.agrojurado.sfmappv2.data.local.dao.EvaluacionGeneralDao
import com.agrojurado.sfmappv2.data.mapper.EvaluacionGeneralMapper
import com.agrojurado.sfmappv2.data.remote.api.EvaluacionGeneralApiService
import com.agrojurado.sfmappv2.data.remote.dto.common.utils.Utils
import com.agrojurado.sfmappv2.data.remote.dto.evaluaciongeneral.EvaluacionGeneralResponse
import com.agrojurado.sfmappv2.domain.model.EvaluacionGeneral
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.domain.repository.EvaluacionGeneralRepository
import com.agrojurado.sfmappv2.domain.repository.EvaluacionPolinizacionRepository
import com.agrojurado.sfmappv2.domain.repository.OperarioRepository
import com.agrojurado.sfmappv2.domain.repository.UsuarioRepository
import com.agrojurado.sfmappv2.domain.security.UserRoleConstants
import com.agrojurado.sfmappv2.utils.SyncNotificationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import java.io.File
import java.util.LinkedList
import java.util.Queue
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

@Singleton
class EvaluacionGeneralRepositoryImpl @Inject constructor(
    private val evaluacionGeneralDao: EvaluacionGeneralDao,
    private val evaluacionGeneralApiService: EvaluacionGeneralApiService,
    private val evaluacionPolinizacionRepository: EvaluacionPolinizacionRepository,
    private val usuarioRepository: UsuarioRepository,
    private val operarioRepository: OperarioRepository,
    @ApplicationContext private val context: Context
) : EvaluacionGeneralRepository {

    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val syncMutex = Mutex()
    private val syncQueue: Queue<Int> = LinkedList()
    private val failedSyncIds = ConcurrentHashMap<Int, Int>()
    private val retryCounts = ConcurrentHashMap<Int, Int>()
    private var isSyncInProgress = false

    private val notificationManager by lazy {
        SyncNotificationManager.getInstance(context)
    }

    companion object {
        private const val TAG = "EvaluacionGeneralRepo"
        private const val MAX_RETRIES = 3
        private const val BATCH_SIZE = 10
    }

    private fun isNetworkAvailable(): Boolean = Utils.isNetworkAvailable(context)

    private suspend fun notifyUser(message: String) {
        withContext(Dispatchers.Main) {
            Utils.showToast(context, message)
        }
    }

    private fun logServerError(response: Response<*>, logMessage: String) {
        Utils.logError(TAG, Exception("Server error (${response.code()}): ${response.errorBody()?.string()}"), logMessage)
    }

    override suspend fun insertEvaluacionGeneral(evaluacionGeneral: EvaluacionGeneral): Long {
        return withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            val localId = evaluacionGeneralDao.insertEvaluacionGeneral(
                EvaluacionGeneralMapper.toDatabase(
                    evaluacionGeneral.copy(
                        isSynced = false,
                        timestamp = timestamp
                    )
                )
            )
            if (!evaluacionGeneral.isTemporary) {
                addToSyncQueue(localId.toInt())
            }
            Log.d(TAG, "Inserted EvaluacionGeneral with local ID $localId, isTemporary: ${evaluacionGeneral.isTemporary}")
            localId
        }
    }

    override suspend fun updateEvaluacionGeneral(evaluacionGeneral: EvaluacionGeneral) {
        withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            val updatedEntity = EvaluacionGeneralMapper.toDatabase(
                evaluacionGeneral.copy(
                    isSynced = false,
                    timestamp = timestamp
                )
            )
            evaluacionGeneralDao.updateEvaluacionGeneral(updatedEntity)
            if (!evaluacionGeneral.isTemporary && evaluacionGeneral.id != null) {
                addToSyncQueue(evaluacionGeneral.id!!)
            }
            notifyUser("Evaluación general actualizada localmente${if (evaluacionGeneral.isTemporary) " (temporal)" else ""}")
        }
    }

    private fun addToSyncQueue(id: Int) {
        if (!syncQueue.contains(id) && !failedSyncIds.contains(id)) {
            syncQueue.add(id)
            Log.d(TAG, "📝 Añadido ID $id a la cola de sincronización (cola: ${syncQueue.size})")
        } else if (failedSyncIds.contains(id)) {
            Log.w(TAG, "🚫 ID $id ya falló permanentemente. No se reencolará.")
        } else {
            Log.d(TAG, "ℹ️ ID $id ya está en la cola de sincronización.")
        }
        
        if (!isSyncInProgress && isNetworkAvailable()) {
            syncScope.launch {
                processSyncQueue()
            }
        }
    }

    private suspend fun processSyncQueue() {
        if (isSyncInProgress || syncQueue.isEmpty()) return
        
        if (!syncMutex.tryLock()) {
            Log.d(TAG, "🔒 ProcessSyncQueue: Mutex ocupado, esperando... (otra sync en progreso)")
            return
        }

        isSyncInProgress = true
        try {
            Log.d(TAG, "🔄 Iniciando procesamiento de cola de sincronización (${syncQueue.size} pendientes)")
            
            val idsToSync = mutableListOf<Int>()
            while (idsToSync.size < BATCH_SIZE && syncQueue.isNotEmpty()) {
                 syncQueue.poll()?.let { id ->
                     if (!failedSyncIds.contains(id)) {
                         idsToSync.add(id)
                     } else {
                         Log.w(TAG, "🚫 Omitiendo ID $id del procesamiento actual, ya falló permanentemente.")
                     }
                 }
            }

            if (idsToSync.isNotEmpty()) {
                Log.d(TAG, "📄 Procesando IDs desde cola: $idsToSync")
                notificationManager.startSyncNotification(
                    "Sincronizando evaluaciones",
                    "Preparando sincronización de ${idsToSync.size} evaluaciones..."
                )
                val failedSyncResult = syncEvaluacionesGeneralesInternal(idsToSync)

                idsToSync.forEach { id ->
                    if (failedSyncResult.containsKey(id)) {
                        val currentRetries = retryCounts.getOrDefault(id, 0) + 1
                        retryCounts[id] = currentRetries
                        if (currentRetries < MAX_RETRIES) {
                            if (!failedSyncIds.containsKey(id)) {
                                syncQueue.add(id)
                                Log.d(TAG, "🔄 Reintentando ID $id (intento $currentRetries/$MAX_RETRIES)")
                            }
                        } else {
                            val errorCode = failedSyncResult[id] ?: -1
                            failedSyncIds.put(id, errorCode)
                            retryCounts.remove(id)
                            Log.w(TAG, "🚫 ID $id alcanzó el máximo de reintentos ($MAX_RETRIES). Marcado como fallido permanentemente (error: $errorCode).")
                            notificationManager.updateSyncMessage("Evaluación ID $id no sincronizada tras $MAX_RETRIES intentos (error: $errorCode)")
                        }
                    } else {
                        retryCounts.remove(id)
                        Log.d(TAG, "✅ ID $id sincronizado correctamente. Eliminado de reintentos.")
                    }
                }
            } else {
                 Log.d(TAG, "ℹ️ No hay IDs válidos para procesar en esta iteración de cola.")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error procesando cola de sincronización: ${e.message}", e)
            notificationManager.errorSyncNotification("Error en sincronización: ${e.message}")
        } finally {
            isSyncInProgress = false
            syncMutex.unlock()
            Log.d(TAG, "🔓 ProcessSyncQueue: Mutex liberado.")
            
            if (syncQueue.isNotEmpty() && isNetworkAvailable()) {
                syncScope.launch {
                    kotlinx.coroutines.delay(1000)
                    Log.d(TAG, "⏳ Programando siguiente ejecución de processSyncQueue...")
                    processSyncQueue()
                }
            } else {
                 Log.d(TAG, "🏁 Cola de sincronización vacía o sin conexión. Deteniendo procesamiento por ahora.")
            }
        }
    }

    private suspend fun syncEvaluacionesGeneralesInternal(specificIds: List<Int>?): Map<Int, Int> {
        if (!isNetworkAvailable()) {
            notifyUser("Sin conexión, sincronización pendiente")
            return emptyMap()
        }

        return withContext(Dispatchers.IO) {
            try {
                val currentUser = usuarioRepository.getLoggedInUserEmail()?.let { email ->
                    usuarioRepository.getUserByEmail(email).first()
                }
                Log.d(TAG, "🔄 Ejecutando lógica interna de sincronización ${specificIds?.let { " para IDs: $it" } ?: "(todos los pendientes)"}")

                // ETAPA 1: PREPARACIÓN DE DATOS
                val toSync = if (specificIds != null) {
                    specificIds.mapNotNull { id ->
                        evaluacionGeneralDao.getEvaluacionGeneralById(id)?.let {
                            EvaluacionGeneralMapper.toDomain(it)
                        }
                    }.filter { !it.isTemporary }
                } else {
                    evaluacionGeneralDao.getUnsyncedEvaluaciones()
                        .map { EvaluacionGeneralMapper.toDomain(it) }
                        .filter { !it.isTemporary }
                }.distinctBy { it.id }

                failedSyncIds.clear()
                val failedSync = mutableMapOf<Int, Int>()
                val serverIdsMap = mutableMapOf<Int, Int>()
                val totalEvaluaciones = toSync.size

                if (totalEvaluaciones == 0) {
                    Log.d(TAG, "✅ No hay evaluaciones pendientes para sincronizar")
                    notificationManager.completeSyncNotification("No hay evaluaciones pendientes")
                    try {
                        notificationManager.updateSyncMessage("Descargando datos del servidor...")
                        fetchEvaluacionesFromServer()
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error al obtener datos desde servidor: ${e.message}", e)
                    }
                    return@withContext emptyMap()
                }

                notificationManager.startSyncNotification(
                    "Sincronizando evaluaciones",
                    "Preparando sincronización de $totalEvaluaciones evaluaciones..."
                )

                // ETAPA 2: ENVÍO DE DATOS
                val batches = toSync.chunked(BATCH_SIZE)
                var currentProgress = 0

                batches.forEachIndexed { batchIndex, batch ->
                    try {
                        notificationManager.updateSyncProgress(
                            currentProgress,
                            totalEvaluaciones,
                            "Lote ${batchIndex + 1}/${batches.size}: ${batch.size} evaluaciones"
                        )

                        batch.forEach { localEval ->
                            currentProgress++
                            if (localEval.id == null) {
                                Log.e(TAG, "❌ Evaluación sin ID local, omitiendo")
                                return@forEach
                            }
                            Log.d(TAG, "Procesando EvaluacionGeneral ID ${localEval.id}, serverId: ${localEval.serverId}")
                            try {
                                val request = EvaluacionGeneralMapper.toRequest(localEval)
                                val response = if (localEval.serverId != null) {
                                    evaluacionGeneralApiService.updateEvaluacionGeneral(localEval.serverId!!, request)
                                } else {
                                    evaluacionGeneralApiService.createEvaluacionGeneral(request)
                                }

                                // ETAPA 3: ACTUALIZACIÓN DEL SERVIDOR
                                if (response.isSuccessful && response.body() != null) {
                                    val serverEval = response.body()!!
                                    if (serverEval.id > 0) {
                                        val syncedEval = localEval.copy(
                                            serverId = serverEval.id,
                                            isSynced = true,
                                            timestamp = serverEval.timestamp
                                        )
                                        if (localEval.fotoPath != null) {
                                            notificationManager.updateSyncMessage("Subiendo foto para evaluación ${localEval.id}")
                                            uploadPhotoToServer(serverEval.id, localEval.fotoPath)
                                        }
                                        if (localEval.firmaPath != null) {
                                            notificationManager.updateSyncMessage("Subiendo firma para evaluación ${localEval.id}")
                                            uploadSignatureToServer(serverEval.id, localEval.firmaPath)
                                        }
                                        evaluacionGeneralDao.updateEvaluacionGeneral(
                                            EvaluacionGeneralMapper.toDatabase(syncedEval)
                                        )
                                        serverIdsMap[localEval.id!!] = serverEval.id

                                        // Sincronizar evaluaciones de polinización asociadas
                                        val polinizaciones = evaluacionPolinizacionRepository
                                            .getEvaluacionesByEvaluacionGeneralId(localEval.id!!)
                                            .first()
                                        if (polinizaciones.isNotEmpty()) {
                                            try {
                                                evaluacionPolinizacionRepository.syncEvaluacionesForGeneral(polinizaciones, serverEval.id)
                                            } catch (e: Exception) {
                                                Log.e(TAG, "❌ Error sincronizando polinizaciones para EvalGeneral ${localEval.id}: ${e.message}", e)
                                                failedSync[localEval.id!!] = 4
                                            }
                                        }
                                        Log.d(TAG, "✅ Sincronizada EvaluacionGeneral ID ${localEval.id} con serverId ${serverEval.id}")
                                    } else {
                                        Log.e(TAG, "❌ ID de servidor inválido: ${serverEval.id}")
                                        failedSync[localEval.id!!] = 1
                                    }
                                } else {
                                    logServerError(response, "❌ Fallo al sincronizar EvaluacionGeneral ${localEval.id}")
                                    failedSync[localEval.id!!] = 2
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error al sincronizar evaluación ${localEval.id}: ${e.message}", e)
                                failedSync[localEval.id!!] = 3
                            }
                            notificationManager.updateSyncProgress(
                                currentProgress,
                                totalEvaluaciones,
                                "Sincronizando evaluación $currentProgress/$totalEvaluaciones"
                            )
                        }
                        if (batchIndex < batches.size - 1) {
                            kotlinx.coroutines.delay(500)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error en lote ${batchIndex + 1}: ${e.message}", e)
                        batch.forEach { failedSync[it.id!!] = 3 }
                    }
                }

                // ETAPA 4: SINCRONIZACIÓN BIDIRECCIONAL
                try {
                    notificationManager.updateSyncMessage("Descargando datos del servidor...")
                    fetchEvaluacionesFromServer()
                    
                    // Asegurar la sincronización bidireccional de las evaluaciones de polinización
                    notificationManager.updateSyncMessage("Descargando evaluaciones de polinización...")
                    try {
                        evaluacionPolinizacionRepository.fetchEvaluacionesFromServer()
                        Log.d(TAG, "✅ Sincronización bidireccional de evaluaciones de polinización completada")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error en sincronización bidireccional de polinización: ${e.message}", e)
                        // Continuamos a pesar del error para no interrumpir el proceso principal
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error al obtener datos desde servidor: ${e.message}", e)
                }

                val message = if (failedSync.isEmpty()) {
                    "$totalEvaluaciones evaluaciones sincronizadas correctamente"
                } else {
                    "${serverIdsMap.size}/$totalEvaluaciones sincronizadas, ${failedSync.size} con errores"
                }
                Log.d(TAG, if (failedSync.isEmpty()) "✅ $message" else "⚠️ $message")
                notificationManager.completeSyncNotification(message)
                notifyUser(message)

                failedSync
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en lógica interna de sincronización: ${e.message}", e)
                notificationManager.errorSyncNotification("Error interno de sincronización: ${e.message}")
                notifyUser("Error interno de sincronización: ${e.message}")
                emptyMap<Int, Int>()
            }
        }
    }

    override suspend fun syncEvaluacionesGenerales(): Map<Int, Int> {
        return syncMutex.withLock {
            isSyncInProgress = true
            Log.d(TAG, "Solicitud de sincronización general (manual/auto) iniciando...")
            try {
                if (!isNetworkAvailable()) {
                    notifyUser("Sin conexión, sincronización pendiente")
                    emptyMap<Int, Int>()
                } else {
                    syncEvaluacionesGeneralesInternal(null)
                }
            } finally {
                 isSyncInProgress = false
                 Log.d(TAG, "Solicitud de sincronización general (manual/auto) finalizada.")
            }
        }
    }

    override suspend fun deleteEvaluacionGeneral(evaluacionGeneral: EvaluacionGeneral) {
        withContext(Dispatchers.IO) {
            try {
                evaluacionGeneralDao.deleteEvaluacionGeneral(EvaluacionGeneralMapper.toDatabase(evaluacionGeneral))
                evaluacionPolinizacionRepository.deleteEvaluacionesByEvaluacionGeneralId(evaluacionGeneral.id!!)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting EvaluacionGeneral: ${e.message}", e)
                throw e
            }
        }
    }

    override suspend fun getEvaluacionGeneralById(id: Int): EvaluacionGeneral? {
        return evaluacionGeneralDao.getEvaluacionGeneralById(id)?.let {
            EvaluacionGeneralMapper.toDomain(it)
        }
    }

    override fun getAllEvaluacionesGenerales(): Flow<List<EvaluacionGeneral>> {
        return evaluacionGeneralDao.getAllEvaluacionesGenerales()
            .map { entities -> entities.map { EvaluacionGeneralMapper.toDomain(it) } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getActiveTemporaryEvaluacion(): EvaluacionGeneral? {
        return evaluacionGeneralDao.getTemporalEvaluacionGeneral()?.let {
            EvaluacionGeneralMapper.toDomain(it)
        }
    }

    override suspend fun finalizeTemporaryEvaluacion(evaluacionId: Int) {
        withContext(Dispatchers.IO) {
            val evaluacion = evaluacionGeneralDao.getEvaluacionGeneralById(evaluacionId)
            evaluacion?.let {
                val updated = EvaluacionGeneralMapper.toDomain(it).copy(isTemporary = false)
                updateEvaluacionGeneral(updated)
                Log.d(TAG, "Finalized temporary EvaluacionGeneral ID $evaluacionId")
                if (isNetworkAvailable()) {
                    syncScope.launch {
                        syncEvaluacionesGenerales()
                    }
                }
            } ?: Log.w(TAG, "No temporary EvaluacionGeneral found with ID $evaluacionId")
        }
    }

    override suspend fun deleteTemporaryEvaluaciones() {
        withContext(Dispatchers.IO) {
            evaluacionGeneralDao.deleteTemporalEvaluacionGeneral()
            Log.d(TAG, "Deleted all temporary EvaluacionesGenerales")
        }
    }

    override suspend fun getUnsyncedEvaluationsCount(): Int {
        return evaluacionGeneralDao.getUnsyncedEvaluationsCount()
    }

    override suspend fun fetchEvaluacionesFromServer() {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "No network available, skipping fetch from server")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val serverEvaluaciones = fetchEvaluacionesFromServerWithResponse()
                val currentUser = usuarioRepository.getLoggedInUserEmail()?.let { email ->
                    usuarioRepository.getUserByEmail(email).first()
                }
                val filteredEvaluaciones = filterEvaluacionesByUserRole(serverEvaluaciones, currentUser)

                evaluacionGeneralDao.transaction {
                    val localEvaluaciones = evaluacionGeneralDao.getAllEvaluacionesGenerales().first()
                    val serverIds = filteredEvaluaciones.map { it.id }.toSet()

                    // Eliminar registros locales sincronizados que no están en el servidor
                    localEvaluaciones.filter { it.isSynced && it.serverId != null && !serverIds.contains(it.serverId) }
                        .forEach { local ->
                            evaluacionGeneralDao.deleteEvaluacionGeneral(local)
                            Log.d(TAG, "Deleted EvaluacionGeneral ID ${local.id} (not found on server)")
                        }

                    // Si el servidor está vacío, eliminar todos los registros locales sincronizados
                    if (filteredEvaluaciones.isEmpty()) {
                        localEvaluaciones.filter { it.isSynced }
                            .forEach { local ->
                                evaluacionGeneralDao.deleteEvaluacionGeneral(local)
                                Log.d(TAG, "Deleted EvaluacionGeneral ID ${local.id} (server empty)")
                            }
                    }

                    // Insertar o actualizar registros del servidor
                    filteredEvaluaciones.forEach { serverEval ->
                        val localEval = localEvaluaciones.find { it.serverId == serverEval.id }
                        val domainEval = EvaluacionGeneralMapper.fromResponse(serverEval)

                        if (localEval == null) {
                            val newLocalId = evaluacionGeneralDao.insertEvaluacionGeneral(
                                EvaluacionGeneralMapper.toDatabase(domainEval).copy(isSynced = true)
                            )
                            Log.d(TAG, "Inserted EvaluacionGeneral from server with local ID $newLocalId")
                        } else if (serverEval.timestamp > localEval.timestamp && !localEval.isTemporary) {
                            evaluacionGeneralDao.updateEvaluacionGeneral(
                                EvaluacionGeneralMapper.toDatabase(domainEval).copy(
                                    id = localEval.id,
                                    isSynced = true
                                )
                            )
                            Log.d(TAG, "Updated EvaluacionGeneral ID ${localEval.id} with server data")
                        }
                    }
                }
                notifyUser("Evaluaciones generales sincronizadas desde el servidor")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching EvaluacionesGenerales from server: ${e.message}", e)
                notifyUser("Error al sincronizar evaluaciones generales: ${e.message}")
            }
        }
    }

    private suspend fun uploadPhotoToServer(evaluacionId: Int, photoPath: String?): String? {
        if (photoPath == null || !isNetworkAvailable()) return null
        val file = File(photoPath)
        if (!file.exists()) return null

        val requestFile = file.asRequestBody("image/png".toMediaTypeOrNull())
        val photoPart = MultipartBody.Part.createFormData("photo", file.name, requestFile)

        val response = evaluacionGeneralApiService.uploadPhoto(evaluacionId, photoPart)
        return if (response.isSuccessful) {
            response.body()?.get("url")?.also { url ->
                Log.d(TAG, "Photo uploaded successfully for EvaluacionGeneral ID $evaluacionId: $url")
            }
        } else {
            logServerError(response, "Failed to upload photo for EvaluacionGeneral ID $evaluacionId")
            null
        }
    }

    private suspend fun uploadSignatureToServer(evaluacionId: Int, signaturePath: String?): String? {
        if (signaturePath == null || !isNetworkAvailable()) return null
        val file = File(signaturePath)
        if (!file.exists()) return null

        val requestFile = file.asRequestBody("image/png".toMediaTypeOrNull())
        val signaturePart = MultipartBody.Part.createFormData("signature", file.name, requestFile)

        val response = evaluacionGeneralApiService.uploadSignature(evaluacionId, signaturePart)
        return if (response.isSuccessful) {
            response.body()?.get("url")?.also { url ->
                Log.d(TAG, "Signature uploaded successfully for EvaluacionGeneral ID $evaluacionId: $url")
            }
        } else {
            logServerError(response, "Failed to upload signature for EvaluacionGeneral ID $evaluacionId")
            null
        }
    }

    private suspend fun downloadImageFromServer(url: String, fileName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val response = evaluacionGeneralApiService.downloadImage(url)
                if (response.isSuccessful) {
                    val file = File(context.filesDir, "$fileName.png")
                    response.body()?.byteStream()?.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d(TAG, "Imagen descargada desde $url y guardada en: ${file.absolutePath}")
                    file.absolutePath
                } else {
                    Log.e(TAG, "Fallo al descargar imagen desde $url: ${response.message()}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error descargando imagen desde $url: ${e.message}", e)
                null
            }
        }
    }

    private suspend fun fetchEvaluacionesFromServerWithResponse(): List<EvaluacionGeneralResponse> {
        return try {
            val serverResponse = evaluacionGeneralApiService.getEvaluacionesGenerales()
            if (!serverResponse.isSuccessful) {
                logServerError(serverResponse, "Failed to fetch EvaluacionesGenerales")
                throw Exception("Error fetching server data: ${serverResponse.message()}")
            }
            serverResponse.body()?.filterNotNull() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching EvaluacionesGenerales from server: ${e.message}", e)
            emptyList()
        }
    }

    private fun filterEvaluacionesByUserRole(
        serverEvaluaciones: List<EvaluacionGeneralResponse>,
        currentUser: Usuario?
    ): List<EvaluacionGeneralResponse> {
        return currentUser?.let { user ->
            when {
                user.rol.equals(UserRoleConstants.ROLE_ADMIN, ignoreCase = true) ||
                        user.rol.equals(UserRoleConstants.ROLE_COORDINATOR, ignoreCase = true) ->
                    serverEvaluaciones
                user.rol.equals(UserRoleConstants.ROLE_EVALUATOR, ignoreCase = true) -> {
                    val operariosEnFinca = runBlocking {
                        operarioRepository.getAllOperarios()
                            .first()
                            .filter { it.fincaId == user.idFinca }
                            .map { it.id }
                    }
                    serverEvaluaciones.filter { it.idpolinizadorev == null || operariosEnFinca.contains(it.idpolinizadorev) }
                }
                else -> emptyList()
            }
        } ?: serverEvaluaciones
    }
}