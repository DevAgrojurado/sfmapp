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
import java.io.File
import javax.inject.Inject

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

    companion object {
        private const val TAG = "EvaluacionGeneralRepository"
    }

    private fun isNetworkAvailable(): Boolean = Utils.isNetworkAvailable(context)

    private suspend fun notifyUser(message: String) {
        withContext(Dispatchers.Main) {
            Utils.showToast(context, message)
        }
    }

    private fun logServerError(response: retrofit2.Response<*>, logMessage: String) {
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
            notifyUser("Evaluación general actualizada localmente${if (evaluacionGeneral.isTemporary) " (temporal)" else ""}")
        }
    }

    override suspend fun deleteEvaluacionGeneral(evaluacionGeneral: EvaluacionGeneral) {
        withContext(Dispatchers.IO) {
            try {
                evaluacionGeneralDao.deleteEvaluacionGeneral(EvaluacionGeneralMapper.toDatabase(evaluacionGeneral))
                evaluacionPolinizacionRepository.deleteEvaluacionesByEvaluacionGeneralId(evaluacionGeneral.id!!)
                notifyUser("Evaluación general eliminada localmente${if (evaluacionGeneral.isTemporary) " (temporal)" else ""}")
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
        val response = evaluacionGeneralApiService.getEvaluacionesGenerales()
        if (response.isSuccessful) {
            response.body()?.let { serverEvaluaciones ->
                val localEvaluaciones = serverEvaluaciones.map { EvaluacionGeneralMapper.fromResponse(it) }
                for (evaluacion in localEvaluaciones) {
                    val existing = evaluacionGeneralDao.getEvaluacionGeneralByServerId(evaluacion.serverId!!)
                    if (existing == null) {
                        insertEvaluacionGeneral(evaluacion.copy(isSynced = true))
                    } else {
                        val localEval = EvaluacionGeneralMapper.toDomain(existing)
                        if (evaluacion.timestamp > localEval.timestamp && !localEval.isTemporary) {
                            updateEvaluacionGeneral(
                                localEval.copy(
                                    fecha = evaluacion.fecha,
                                    hora = evaluacion.hora,
                                    semana = evaluacion.semana,
                                    idevaluadorev = evaluacion.idevaluadorev,
                                    idpolinizadorev = evaluacion.idpolinizadorev,
                                    idLoteev = evaluacion.idLoteev,
                                    fotoPath = evaluacion.fotoPath,
                                    firmaPath = evaluacion.firmaPath,
                                    timestamp = evaluacion.timestamp,
                                    isSynced = true
                                )
                            )
                        }
                    }
                }
            }
        } else {
            logServerError(response, "Failed to fetch EvaluacionesGenerales")
            throw Exception("Failed to fetch evaluations: ${response.message()}")
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

    override suspend fun syncEvaluacionesGenerales(): Map<Int, Int> {
        if (!isNetworkAvailable()) {
            notifyUser("Sin conexión, sincronización pendiente")
            return emptyMap()
        }

        return withContext(Dispatchers.IO) {
            syncMutex.withLock {
                try {
                    val currentUser = usuarioRepository.getLoggedInUserEmail()?.let { email ->
                        usuarioRepository.getUserByEmail(email).first()
                    }
                    Log.d(TAG, "Iniciando sincronización de EvaluacionesGenerales")

                    val unsyncedGenerales = evaluacionGeneralDao.getUnsyncedEvaluacionesGenerales()
                        .map { EvaluacionGeneralMapper.toDomain(it) }
                        .filter { !it.isTemporary }
                    val serverIdsMap = mutableMapOf<Int, Int>()

                    unsyncedGenerales.forEach { localEval ->
                        Log.d(TAG, "Sincronizando EvaluacionGeneral ID ${localEval.id}, fotoPath inicial: ${localEval.fotoPath}")
                        val request = EvaluacionGeneralMapper.toRequest(localEval)
                        val response = if (localEval.serverId != null) {
                            evaluacionGeneralApiService.updateEvaluacionGeneral(localEval.serverId!!, request)
                        } else {
                            evaluacionGeneralApiService.createEvaluacionGeneral(request)
                        }

                        if (response.isSuccessful && response.body() != null) {
                            val serverEval = response.body()!!
                            if (serverEval.id > 0) {
                                serverIdsMap[localEval.id!!] = serverEval.id
                                val photoUrl = uploadPhotoToServer(serverEval.id, localEval.fotoPath)
                                val signatureUrl = uploadSignatureToServer(serverEval.id, localEval.firmaPath)

                                // Descargar la imagen y asegurarse de que fotoPath sea el path local
                                val localPhotoPath = if (photoUrl != null) {
                                    downloadImageFromServer(photoUrl, "foto_${serverEval.id}")
                                        ?: localEval.fotoPath // Usa el path original si la descarga falla
                                } else {
                                    localEval.fotoPath
                                }
                                Log.d(TAG, "Photo URL: $photoUrl, Local photo path: $localPhotoPath")

                                val localSignaturePath = if (signatureUrl != null) {
                                    downloadImageFromServer(signatureUrl, "firma_${serverEval.id}")
                                        ?: localEval.firmaPath
                                } else {
                                    localEval.firmaPath
                                }

                                val updatedEval = localEval.copy(
                                    serverId = serverEval.id,
                                    fotoPath = localPhotoPath, // Siempre guarda el path local
                                    firmaPath = localSignaturePath,
                                    isSynced = true,
                                    timestamp = serverEval.timestamp
                                )
                                evaluacionGeneralDao.updateEvaluacionGeneral(
                                    EvaluacionGeneralMapper.toDatabase(updatedEval)
                                )
                                Log.d(TAG, "EvaluacionGeneral ID ${localEval.id} sincronizada con serverId ${serverEval.id}, fotoPath actualizado: ${updatedEval.fotoPath}")
                            }
                        } else {
                            logServerError(response, "Fallo al sincronizar EvaluacionGeneral ${localEval.id}")
                        }
                    }

                    val serverEvaluaciones = fetchEvaluacionesFromServerWithResponse()
                    val filteredEvaluaciones = filterEvaluacionesByUserRole(serverEvaluaciones, currentUser)
                    updateLocalDatabaseWithServerData(filteredEvaluaciones)

                    serverIdsMap.forEach { (localId, serverId) ->
                        val polinizaciones = evaluacionPolinizacionRepository
                            .getEvaluacionesByEvaluacionGeneralId(localId)
                            .first()
                        if (polinizaciones.isNotEmpty()) {
                            evaluacionPolinizacionRepository.syncEvaluacionesForGeneral(polinizaciones, serverId)
                        }
                    }

                    evaluacionPolinizacionRepository.fetchEvaluacionesFromServer()

                    notifyUser("Sincronización completada con éxito")
                    Log.d(TAG, "Sincronización completada con éxito")
                    serverIdsMap
                } catch (e: Exception) {
                    Log.e(TAG, "Error de sincronización: ${e.message}", e)
                    notifyUser("Error durante la sincronización: ${e.message}")
                    emptyMap()
                }
            }
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

    private suspend fun updateLocalDatabaseWithServerData(serverEvaluaciones: List<EvaluacionGeneralResponse>) {
        evaluacionGeneralDao.transaction {
            val localEvaluaciones = evaluacionGeneralDao.getAllEvaluacionesGenerales().first()
            val serverIds = serverEvaluaciones.map { it.id }.toSet()

            // Eliminar EvaluacionesGenerales locales que no están en el servidor, pero solo si no tienen polinizaciones asociadas
            localEvaluaciones.filter { it.serverId != null && !serverIds.contains(it.serverId) }
                .forEach { local ->
                    val polinizaciones = evaluacionPolinizacionRepository
                        .getEvaluacionesByEvaluacionGeneralId(local.id)
                        .first()
                    if (polinizaciones.isEmpty()) {
                        evaluacionGeneralDao.deleteEvaluacionGeneral(local)
                        Log.d(TAG, "Deleted EvaluacionGeneral ID ${local.id} (not found on server)")
                    } else {
                        Log.w(TAG, "Keeping EvaluacionGeneral ID ${local.id} with ${polinizaciones.size} associated polinizaciones")
                    }
                }

            // Actualizar o insertar EvaluacionesGenerales del servidor
            serverEvaluaciones.forEach { serverEval ->
                val localEval = localEvaluaciones.find { it.serverId == serverEval.id }
                val domainEval = EvaluacionGeneralMapper.fromResponse(serverEval)

                if (localEval != null) {
                    evaluacionGeneralDao.updateEvaluacionGeneral(
                        EvaluacionGeneralMapper.toDatabase(domainEval)
                            .copy(id = localEval.id, isSynced = true)
                    )
                    Log.d(TAG, "Updated EvaluacionGeneral ID ${localEval.id} with server data")
                } else {
                    val newLocalId = evaluacionGeneralDao.insertEvaluacionGeneral(
                        EvaluacionGeneralMapper.toDatabase(domainEval)
                            .copy(isSynced = true)
                    )
                    Log.d(TAG, "Inserted EvaluacionGeneral from server with local ID $newLocalId")
                }
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