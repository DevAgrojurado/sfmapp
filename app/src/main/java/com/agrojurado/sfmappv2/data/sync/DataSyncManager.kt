package com.agrojurado.sfmappv2.data.sync

import android.content.Context
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.work.*
import com.agrojurado.sfmappv2.data.local.dao.EvaluacionGeneralDao
import com.agrojurado.sfmappv2.data.local.dao.EvaluacionPolinizacionDao
import com.agrojurado.sfmappv2.data.local.dao.SyncQueueDao
import com.agrojurado.sfmappv2.data.local.entity.EvaluacionGeneralEntity
import com.agrojurado.sfmappv2.data.local.entity.EvaluacionPolinizacionEntity
import com.agrojurado.sfmappv2.data.local.entity.SyncQueueEntity
import com.agrojurado.sfmappv2.data.mapper.EvaluacionGeneralMapper
import com.agrojurado.sfmappv2.data.mapper.EvaluacionPolinizacionMapper
import com.agrojurado.sfmappv2.data.remote.api.EvaluacionApiService
import com.agrojurado.sfmappv2.data.remote.api.EvaluacionGeneralApiService
import com.agrojurado.sfmappv2.data.remote.dto.evaluacion.EvaluacionRequest
import com.agrojurado.sfmappv2.data.remote.dto.evaluacion.EvaluacionResponse
import com.agrojurado.sfmappv2.data.remote.dto.evaluaciongeneral.EvaluacionGeneralResponse
import com.agrojurado.sfmappv2.domain.repository.*
import com.agrojurado.sfmappv2.utils.SyncNotificationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSyncManager @Inject constructor(
    private val loteRepository: LoteRepository,
    private val fincaRepository: FincaRepository,
    private val areaRepository: AreaRepository,
    private val cargoRepository: CargoRepository,
    private val operarioRepository: OperarioRepository,
    private val usuarioRepository: UsuarioRepository,
    private val evaluacionGeneralDao: EvaluacionGeneralDao,
    private val evaluacionPolinizacionDao: EvaluacionPolinizacionDao,
    private val syncQueueDao: SyncQueueDao,
    private val evaluacionGeneralApiService: EvaluacionGeneralApiService,
    private val evaluacionApiService: EvaluacionApiService,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DataSyncManager"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 5000L
        private const val MAX_QUEUE_CYCLES = 5
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val notificationManager = SyncNotificationManager.getInstance(context)

    // Flujo para emitir el progreso de sincronización
    data class SyncProgress(val current: Int, val total: Int, val message: String)
    private val _syncProgress = MutableSharedFlow<SyncProgress>(replay = 1)
    val syncProgress = _syncProgress.asSharedFlow()

    public fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities != null && capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando conexión: ${e.message}")
            false
        }
    }

    fun syncAllData(progressBar: ProgressBar, onSyncComplete: () -> Unit) {
        if (!isNetworkAvailable()) {
            coroutineScope.launch {
                _syncProgress.emit(SyncProgress(0, 0, "Sin conexión a Internet"))
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Sin conexión a Internet", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "No hay conexión a Internet disponible para sincronización")
                    progressBar.visibility = ProgressBar.GONE
                    onSyncComplete()
                }
            }
            return
        }

        notificationManager.startSyncNotification(
            "Sincronizando datos",
            "Preparando sincronización..."
        )

        coroutineScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.VISIBLE
                    progressBar.isIndeterminate = false // Asegurar modo determinado
                    progressBar.progress = 0
                }

                // Definir todas las operaciones de sincronización
                val syncOperations = listOf<suspend (Int, Int) -> Unit>(
                    { index, total -> syncData("fincas", progressBar, index, total) { fincaRepository.syncFincas() } },
                    { index, total -> syncData("áreas", progressBar, index, total) { areaRepository.syncAreas() } },
                    { index, total -> syncData("cargos", progressBar, index, total) { cargoRepository.syncCargos() } },
                    { index, total -> syncData("lotes", progressBar, index, total) { loteRepository.syncLotes() } },
                    { index, total -> syncData("operarios", progressBar, index, total) { operarioRepository.syncOperarios() } },
                    { index, total -> syncData("usuarios", progressBar, index, total) { usuarioRepository.syncUsuarios() } },
                    { index, total -> syncEvaluacionesGeneralesFromServer(index, total) },
                    { index, total -> syncEvaluacionesPolinizacionFromServer(index, total) }
                )

                val totalOperations = syncOperations.size + 1 // +1 para la cola
                for ((index, syncOperation) in syncOperations.withIndex()) {
                    syncOperation(index, totalOperations)
                    withContext(Dispatchers.Main) {
                        progressBar.progress = ((index + 1) * 100) / totalOperations
                    }
                }

                // Procesar la cola de sincronización
                syncQueue()

                // Emitir evento final de sincronización completa
                _syncProgress.emit(SyncProgress(totalOperations, totalOperations, "✅ Sincronización completa"))
                notificationManager.completeSyncNotification("Sincronización completa")
                Log.d(TAG, "✅ Sincronización completa")

                withContext(Dispatchers.Main) {
                    onSyncComplete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en proceso de sincronización: ${e.message}", e)
                notificationManager.errorSyncNotification("Error en sincronización: ${e.message}")
                _syncProgress.emit(SyncProgress(0, 0, "❌ Error en sincronización: ${e.message}"))

                withContext(Dispatchers.Main) {
                    onSyncComplete()
                }
            }
        }
    }

    private suspend fun syncData(
        dataType: String,
        progressBar: ProgressBar,
        currentIndex: Int,
        totalOperations: Int,
        syncOperation: suspend () -> Unit
    ) {
        try {
            val progress = (currentIndex * 100) / totalOperations
            notificationManager.updateSyncProgress(
                progress,
                100,
                "Sincronizando $dataType..."
            )

            syncOperation()

            notificationManager.updateSyncProgress(
                ((currentIndex + 1) * 100) / totalOperations,
                100,
                "Completada sincronización de $dataType"
            )
            Log.d(TAG, "✅ Sincronización de $dataType completada")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sincronizando $dataType: ${e.message}", e)
            notificationManager.updateSyncMessage("Error al sincronizar $dataType")
            throw e
        }
    }

    private suspend fun syncEvaluacionesGeneralesFromServer(currentIndex: Int, totalOperations: Int) {
        try {

            val progress = (currentIndex * 100) / totalOperations
            _syncProgress.emit(SyncProgress(progress, 100, "Verificando evaluaciones"))
            notificationManager.updateSyncProgress(progress, 100, "Verificando evaluaciones...")

            // Obtener el usuario logueado para filtrar
            val loggedInUserEmail = usuarioRepository.getLoggedInUserEmail()
            val loggedInUser = loggedInUserEmail?.let { usuarioRepository.getUserByEmail(it).first() }
            val isAdminOrCoordinator = loggedInUser?.rol?.let {
                it.equals("ROLE_ADMIN", ignoreCase = true) || it.equals("ROLE_COORDINATOR", ignoreCase = true)
            } ?: false
            val userFincaId = loggedInUser?.idFinca
            val userId = loggedInUser?.id

            // Sincronizar explícitamente todas las dependencias necesarias
            if (!isAdminOrCoordinator && userFincaId != null) {
                Log.d(TAG, "Sincronizando dependencias explícitamente para usuario no administrador")
                try {
                    usuarioRepository.syncUsuarios()
                    Log.d(TAG, "✅ Usuarios sincronizados")
                    operarioRepository.syncOperarios()
                    Log.d(TAG, "✅ Operarios sincronizados")
                    loteRepository.syncLotes()
                    Log.d(TAG, "✅ Lotes sincronizados")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error sincronizando dependencias: ${e.message}", e)
                    throw Exception("Error al sincronizar dependencias necesarias: ${e.message}")
                }
            }

            // Obtener operarios válidos para el usuario
            val validOperarioIds = if (!isAdminOrCoordinator && userFincaId != null) {
                val operarios = operarioRepository.getAllOperarios().first()
                Log.d(TAG, "Operarios disponibles: ${operarios.size}, Finca ID: $userFincaId")
                operarios
                    .filter { it.fincaId == userFincaId }
                    .map { it.id }
                    .toSet()
                    .also { Log.d(TAG, "Operarios válidos (IDs): $it") }
            } else {
                emptySet()
            }

            // Obtener todas las evaluaciones locales sincronizadas para comparar
            val localSyncedEvals = evaluacionGeneralDao.getAllEvaluacionesGenerales().first()
                .filter { it.syncStatus == "SYNCED" && it.serverId != null }
                .associateBy { it.serverId!! }
                .toMutableMap()
            Log.d(TAG, "📍 Evaluaciones Generales locales sincronizadas: ${localSyncedEvals.size}")

            val response = evaluacionGeneralApiService.getEvaluacionesGenerales()
            if (response.isSuccessful && response.body() != null) {
                val serverEvaluations = response.body()!!
                Log.d(TAG, "📥 Descargadas ${serverEvaluations.size} Evaluaciones Generales")

                val serverIds = mutableSetOf<Int>()
                withContext(Dispatchers.IO) {
                    evaluacionGeneralDao.transaction {
                        serverEvaluations.forEach { serverEval ->
                            serverEval.id?.let { serverIds.add(it) }

                            // Filtrar evaluaciones según el rol del usuario
                            val shouldSync = if (isAdminOrCoordinator) {
                                true
                            } else {
                                val isRelevant = serverEval.idpolinizadorev?.let { validOperarioIds.contains(it) } ?: false ||
                                        serverEval.idevaluadorev == userId
                                Log.d(TAG, "Evaluación serverId ${serverEval.id}: shouldSync=$isRelevant, " +
                                        "idPolinizador=${serverEval.idpolinizadorev}, idEvaluador=${serverEval.idevaluadorev}, userId=$userId")
                                isRelevant
                            }

                            if (!shouldSync) {
                                Log.d(TAG, "⏩ Omitiendo EvaluacionGeneral serverId ${serverEval.id} (no relevante para usuario)")
                                return@forEach
                            }

                            // Verificar que las claves foráneas existan
                            val evaluadorExists = serverEval.idevaluadorev?.let {
                                usuarioRepository.getUserById(it) != null
                            } ?: true
                            val operarioExists = serverEval.idpolinizadorev?.let {
                                operarioRepository.getOperarioById(it) != null
                            } ?: true
                            val loteExists = serverEval.idloteev?.let {
                                loteRepository.getLoteById(it) != null
                            } ?: true

                            if (!evaluadorExists || !operarioExists || !loteExists) {
                                Log.w(TAG, "⚠️ Omitiendo EvaluacionGeneral serverId ${serverEval.id} debido a claves foráneas faltantes: " +
                                        "evaluador=$evaluadorExists (ID: ${serverEval.idevaluadorev}), " +
                                        "operario=$operarioExists (ID: ${serverEval.idpolinizadorev}), " +
                                        "lote=$loteExists (ID: ${serverEval.idloteev})")
                                // Encolar para reintento posterior
                                val domainEval = EvaluacionGeneralMapper.fromResponse(serverEval).copy(
                                    syncStatus = "PENDING",
                                    timestamp = serverEval.timestamp,
                                    serverId = serverEval.id
                                )
                                val entity = EvaluacionGeneralMapper.toDatabase(domainEval)
                                evaluacionGeneralDao.insertEvaluacionGeneral(entity)
                                syncQueueDao.enqueue(SyncQueueEntity(
                                    entityType = "EvaluacionGeneral",
                                    entityId = entity.id
                                ))
                                Log.d(TAG, "📋 Encolada EvaluacionGeneral serverId ${serverEval.id} para reintento")
                                return@forEach
                            }

                            val localEval = serverEval.id?.let { localSyncedEvals[it] }
                            if (localEval != null && localEval.timestamp >= serverEval.timestamp) {
                                Log.d(TAG, "⏩ EvaluacionGeneral serverId ${serverEval.id} ya sincronizada localmente con timestamp reciente")
                                localSyncedEvals.remove(serverEval.id)
                                return@forEach
                            }

                            val domainEval = EvaluacionGeneralMapper.fromResponse(serverEval).copy(
                                syncStatus = "SYNCED",
                                timestamp = serverEval.timestamp,
                                serverId = serverEval.id
                            )
                            val entity = EvaluacionGeneralMapper.toDatabase(domainEval)

                            if (localEval == null) {
                                evaluacionGeneralDao.insertEvaluacionGeneral(entity)
                                Log.d(TAG, "➕ Insertada EvaluacionGeneral serverId ${serverEval.id}")
                            } else {
                                evaluacionGeneralDao.updateEvaluacionGeneral(entity.copy(id = localEval.id))
                                Log.d(TAG, "🔄 Actualizada EvaluacionGeneral serverId ${serverEval.id}")
                                localSyncedEvals.remove(serverEval.id)
                            }
                        }

                        // Eliminar evaluaciones locales sincronizadas que ya no existen en el servidor
                        localSyncedEvals.forEach { (serverId, localEval) ->
                            if (!serverIds.contains(serverId)) {
                                evaluacionGeneralDao.deleteEvaluacionGeneral(localEval)
                                Log.d(TAG, "🗑️ Eliminada EvaluacionGeneral local ID ${localEval.id} (serverId $serverId) no encontrada en el servidor")
                            }
                        }
                    }
                }
                _syncProgress.emit(SyncProgress((currentIndex + 1) * 100 / totalOperations, 100, "Evaluaciones Generales descargadas"))
                notificationManager.updateSyncProgress((currentIndex + 1) * 100 / totalOperations, 100, "Evaluaciones Generales descargadas")
                Log.d(TAG, "✅ Descarga de Evaluaciones Generales completada")
            } else {
                throw Exception("Error al descargar Evaluaciones Generales: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error descargando Evaluaciones Generales: ${e.message}", e)
            _syncProgress.emit(SyncProgress(currentIndex, totalOperations, "❌ Error descargando Evaluaciones Generales"))
            notificationManager.updateSyncMessage("Error al descargar Evaluaciones Generales")
            throw e
        }
    }

    private suspend fun syncEvaluacionesPolinizacionFromServer(currentIndex: Int, totalOperations: Int) {
        try {

            val progress = (currentIndex * 100) / totalOperations
            _syncProgress.emit(SyncProgress(progress, 100, "Verificando eventos"))
            notificationManager.updateSyncProgress(progress, 100, "verificando eventos")

            // Obtener todas las evaluaciones locales sincronizadas para comparar
            val localSyncedEvals = evaluacionPolinizacionDao.getEvaluaciones().first()
                .filter { it.syncStatus == "SYNCED" && it.serverId != null }
                .associateBy { it.serverId!! }
                .toMutableMap()
            Log.d(TAG, "📍 Evaluaciones Polinizacion locales sincronizadas: ${localSyncedEvals.size}")

            val response = evaluacionApiService.getEvaluaciones()
            if (response.isSuccessful && response.body() != null) {
                val serverEvaluations = response.body()!!
                Log.d(TAG, "📥 Descargadas ${serverEvaluations.size} Evaluaciones de Polinización")

                val serverIds = mutableSetOf<Int>()
                withContext(Dispatchers.IO) {
                    evaluacionPolinizacionDao.transaction {
                        serverEvaluations.forEach { serverEval ->
                            serverEval.id?.let { serverIds.add(it) }

                            // Mapear evaluacionGeneralId del servidor al ID local
                            val localGeneralEval = serverEval.evaluaciongeneralid?.let {
                                evaluacionGeneralDao.getEvaluacionGeneralByServerId(it)
                            }
                            if (localGeneralEval == null && serverEval.evaluaciongeneralid != null) {
                                Log.w(TAG, "⚠️ EvaluacionGeneral serverId ${serverEval.evaluaciongeneralid} no encontrada localmente, omitiendo EvaluacionPolinizacion serverId ${serverEval.id}")
                                return@forEach
                            }

                            val localEval = serverEval.id?.let { localSyncedEvals[it] }
                            if (localEval != null && localEval.timestamp >= serverEval.timestamp) {
                                Log.d(TAG, "⏩ EvaluacionPolinizacion serverId ${serverEval.id} ya sincronizada localmente con timestamp reciente")
                                localSyncedEvals.remove(serverEval.id)
                                return@forEach
                            }

                            val domainEval = EvaluacionPolinizacionMapper.fromResponse(serverEval).copy(
                                syncStatus = "SYNCED",
                                timestamp = serverEval.timestamp,
                                serverId = serverEval.id,
                                evaluacionGeneralId = localGeneralEval?.id
                            )
                            val entity = EvaluacionPolinizacionMapper.toDatabase(domainEval)

                            if (localEval == null) {
                                evaluacionPolinizacionDao.insertEvaluacion(entity)
                                Log.d(TAG, "➕ Insertada EvaluacionPolinizacion serverId ${serverEval.id}")
                            } else {
                                evaluacionPolinizacionDao.updateEvaluacion(entity.copy(id = localEval.id))
                                Log.d(TAG, "🔄 Actualizada EvaluacionPolinizacion serverId ${serverEval.id}")
                                localSyncedEvals.remove(serverEval.id)
                            }
                        }

                        // Eliminar evaluaciones locales sincronizadas que ya no existen en el servidor
                        localSyncedEvals.forEach { (serverId, localEval) ->
                            if (!serverIds.contains(serverId)) {
                                evaluacionPolinizacionDao.deleteEvaluacion(localEval)
                                Log.d(TAG, "🗑️ Eliminada EvaluacionPolinizacion local ID ${localEval.id} (serverId $serverId) no encontrada en el servidor")
                            }
                        }
                    }
                }
                _syncProgress.emit(SyncProgress((currentIndex + 1) * 100 / totalOperations, 100, "Evaluaciones de Polinización descargadas"))
                notificationManager.updateSyncProgress((currentIndex + 1) * 100 / totalOperations, 100, "Evaluaciones de Polinización descargadas")
                Log.d(TAG, "✅ Descarga de Evaluaciones de Polinización completada")
            } else {
                throw Exception("Error al descargar Evaluaciones de Polinización: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error descargando Evaluaciones de Polinización: ${e.message}", e)
            _syncProgress.emit(SyncProgress(currentIndex, totalOperations, "❌ Error descargando Evaluaciones de Polinización"))
            notificationManager.updateSyncMessage("Error al descargar Evaluaciones de Polinización")
            throw e
        }
    }

    private suspend fun syncQueue() {
        var cycleCount = 0
        var successfulSyncs: Int
        var remainingCount: Int

        do {
            cycleCount++
            Log.d(TAG, "🔄 Iniciando ciclo de sincronización $cycleCount")

            enqueueUnsyncedEvaluaciones()

            val queueItems = syncQueueDao.getQueue()
                .first()
                .sortedBy { it.entityType != "EvaluacionGeneral" }

            if (queueItems.isEmpty()) {
                updateSyncStatus(0, 0, "✅ Cola de sincronización vacía")
                return
            }

            Log.d(TAG, "📋 Procesando ${queueItems.size} elementos en la cola (ciclo $cycleCount)")
            withContext(Dispatchers.Main) {
                notificationManager.updateSyncMessage("Procesando ${queueItems.size} elementos en la cola...")
            }

            successfulSyncs = 0
            remainingCount = queueItems.size

            for ((index, queueItem) in queueItems.withIndex()) {
                withContext(Dispatchers.IO) {
                    syncQueueDao.transaction {
                        val wasSuccessful = processQueueItem(queueItem, index, queueItems.size)
                        if (wasSuccessful) {
                            successfulSyncs++
                            remainingCount--
                            removeItemIfSynced(queueItem)
                        }
                    }
                }

                enqueueUnsyncedEvaluaciones()

                if (!isNetworkAvailable()) {
                    Log.w(TAG, "⚠️ Sin conexión, deteniendo sincronización")
                    // No emitir ninguna notificación ni mensaje
                    break
                }

                updateProgressDuringSync(index, queueItems.size)
            }

            // Verificar elementos restantes para decidir si continuar el ciclo
            remainingCount = syncQueueDao.getQueue().first().size

        } while (remainingCount > 0 && successfulSyncs > 0 && cycleCount < MAX_QUEUE_CYCLES)

        if (cycleCount >= MAX_QUEUE_CYCLES) {
            Log.w(TAG, "⚠️ Alcanzado límite de ciclos ($MAX_QUEUE_CYCLES), deteniendo sincronización")
        }
    }

    private suspend fun processQueueItem(queueItem: SyncQueueEntity, index: Int, totalItems: Int): Boolean {
        if (!isNetworkAvailable()) {
            //updateSyncStatus(index, totalItems, "⚠️ Sin conexión, deteniendo procesamiento")
            Log.w(TAG, "⚠️ Sin conexión, deteniendo procesamiento de cola")
            notificationManager.updateSyncMessage("Sin conexión, sincronización pausada")
            return false
        }

        if (isItemAlreadySynced(queueItem)) {
            syncQueueDao.dequeue(queueItem.entityType, queueItem.entityId)
            Log.d(TAG, "✅ ${queueItem.entityType} ID ${queueItem.entityId} ya sincronizada, eliminando de la cola")
            return true
        }

        try {
            updateProcessingProgress(queueItem, index, totalItems)

            when (queueItem.entityType) {
                "EvaluacionGeneral" -> syncEvaluacionGeneral(queueItem)
                "EvaluacionPolinizacion" -> syncEvaluacionPolinizacion(queueItem)
                else -> {
                    Log.w(TAG, "Tipo de entidad desconocido: ${queueItem.entityType}")
                    syncQueueDao.dequeue(queueItem.entityType, queueItem.entityId)
                    return true
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error al sincronizar ${queueItem.entityType} ID ${queueItem.entityId}: ${e.message}", e)
            _syncProgress.emit(
                SyncProgress(
                    index + 1,
                    totalItems,
                    "Error al sincronizar ${queueItem.entityType} ID ${queueItem.entityId}: ${e.message}"
                )
            )
            handleSyncFailure(queueItem)
            return false
        }
    }

    private suspend fun enqueueUnsyncedEvaluaciones() {
        withContext(Dispatchers.IO) {
            syncQueueDao.transaction {
                val existingQueueItems = syncQueueDao.getQueue().first()
                    .associateBy { "${it.entityType}_${it.entityId}" }

                // Encolar Evaluaciones Generales
                val unsyncedGenerales = evaluacionGeneralDao.getUnsyncedEvaluaciones()
                    .filter { eval ->
                        val key = "EvaluacionGeneral_${eval.id}"
                        eval.syncStatus in listOf("PENDING", "FAILED") &&
                                !(eval.syncStatus == "SYNCED" && eval.serverId != null) &&
                                eval.syncStatus != "SYNCING" &&
                                existingQueueItems[key] == null
                    }
                unsyncedGenerales.forEach { eval ->
                    syncQueueDao.enqueueIfNotExists(
                        SyncQueueEntity(entityType = "EvaluacionGeneral", entityId = eval.id)
                    )
                    Log.d(TAG, "📋 Encolada EvaluacionGeneral ID ${eval.id}")
                }

                // Encolar Evaluaciones de Polinización
                val unsyncedPolinizaciones = evaluacionPolinizacionDao.getEvaluaciones().first()
                    .filter { eval ->
                        val key = "EvaluacionPolinizacion_${eval.id}"
                        eval.syncStatus in listOf("PENDING", "FAILED") &&
                                !(eval.syncStatus == "SYNCED" && eval.serverId != null) &&
                                eval.syncStatus != "SYNCING" &&
                                existingQueueItems[key] == null
                    }
                unsyncedPolinizaciones.forEach { eval ->
                    if (eval.evaluacionGeneralId != null) {
                        val generalEval = evaluacionGeneralDao.getEvaluacionGeneralById(eval.evaluacionGeneralId!!)
                        if (generalEval == null || generalEval.syncStatus != "SYNCED" || generalEval.serverId == null) {
                            Log.w(TAG, "⚠️ No encolando EvaluacionPolinizacion ID ${eval.id} porque EvaluacionGeneral ID ${eval.evaluacionGeneralId} no está sincronizada")
                            if (generalEval != null && generalEval.syncStatus != "SYNCED") {
                                val generalKey = "EvaluacionGeneral_${generalEval.id}"
                                if (existingQueueItems[generalKey] == null && generalEval.syncStatus != "SYNCING") {
                                    syncQueueDao.enqueueIfNotExists(
                                        SyncQueueEntity(entityType = "EvaluacionGeneral", entityId = generalEval.id)
                                    )
                                    Log.d(TAG, "📋 Encolada EvaluacionGeneral ID ${generalEval.id} para EvaluacionPolinizacion ID ${eval.id}")
                                }
                            }
                            return@forEach
                        }
                    }
                    syncQueueDao.enqueueIfNotExists(
                        SyncQueueEntity(entityType = "EvaluacionPolinizacion", entityId = eval.id)
                    )
                    Log.d(TAG, "📋 Encolada EvaluacionPolinizacion ID ${eval.id}")
                }

                val queueCount = syncQueueDao.getQueue().first().size
                Log.d(TAG, "📋 Total en cola después de encolar: $queueCount elementos")
            }
        }
    }

    // Métodos auxiliares
    private suspend fun updateSyncStatus(current: Int, total: Int, message: String) {
        _syncProgress.emit(SyncProgress(current, total, message))
        if (current == 0 && total == 0) {
            notificationManager.updateSyncMessage(message)
        }
        Log.d(TAG, message)
    }

    private suspend fun updateProgressDuringSync(index: Int, totalItems: Int) {
        val updatedQueueItems = syncQueueDao.getQueue().first()
        val remainingCount = updatedQueueItems.size
        val currentProgress = index + 1
        val percentage = ((currentProgress.toFloat() / totalItems.toFloat()) * 100).toInt()

        val progressMessage = if (remainingCount > 0) {
            "Procesado ${index + 1} de $totalItems, quedan $remainingCount elementos"
        } else {
            "✅ Sincronización completa"
        }

        val notificationMessage = if (remainingCount > 0) {
            "$currentProgress de $totalItems ($percentage%)"
        } else {
            "✅ Sincronización completa"
        }

        _syncProgress.emit(SyncProgress(index + 1, totalItems, progressMessage))
        notificationManager.updateSyncProgress(index + 1, totalItems, notificationMessage)
        Log.d(TAG, progressMessage)
    }

    private suspend fun updateFinalSyncStatus(remainingCount: Int, cycleCount: Int, totalItems: Int) {
        val finalMessage = if (remainingCount > 0) {
            "⚠️ Quedan $remainingCount elementos pendientes tras ciclo $cycleCount"
        } else {
            "✅ Sincronización completa"
        }
        _syncProgress.emit(SyncProgress(totalItems, totalItems, finalMessage))
        notificationManager.updateSyncMessage(finalMessage)
        Log.d(TAG, finalMessage)
    }

    private suspend fun isItemAlreadySynced(queueItem: SyncQueueEntity): Boolean {
        return when (queueItem.entityType) {
            "EvaluacionGeneral" -> {
                evaluacionGeneralDao.getEvaluacionGeneralById(queueItem.entityId)
                    ?.let { it.syncStatus == "SYNCED" && it.serverId != null } ?: false
            }
            "EvaluacionPolinizacion" -> {
                evaluacionPolinizacionDao.getEvaluacionById(queueItem.entityId)
                    ?.let { it.syncStatus == "SYNCED" && it.serverId != null } ?: false
            }
            else -> false
        }
    }

    private suspend fun removeItemIfSynced(queueItem: SyncQueueEntity) {
        when (queueItem.entityType) {
            "EvaluacionGeneral" -> {
                val eval = evaluacionGeneralDao.getEvaluacionGeneralById(queueItem.entityId)
                if (eval?.syncStatus == "SYNCED" && eval.serverId != null) {
                    syncQueueDao.dequeue(queueItem.entityType, queueItem.entityId)
                    Log.d(TAG, "✅ Eliminada EvaluacionGeneral ID ${queueItem.entityId} de la cola tras sincronización")
                }
            }
            "EvaluacionPolinizacion" -> {
                val eval = evaluacionPolinizacionDao.getEvaluacionById(queueItem.entityId)
                if (eval?.syncStatus == "SYNCED" && eval.serverId != null) {
                    syncQueueDao.dequeue(queueItem.entityType, queueItem.entityId)
                    Log.d(TAG, "✅ Eliminada EvaluacionPolinizacion ID ${queueItem.entityId} de la cola tras sincronización")
                }
            }
        }
    }

    private suspend fun updateProcessingProgress(queueItem: SyncQueueEntity, index: Int, totalItems: Int) {
        val message = "Sincronizando (${index + 1}/$totalItems)"
        val currentProgress = index + 1
        val percentage = ((currentProgress.toFloat() / totalItems.toFloat()) * 100).toInt()
        val notificationMessage = "$currentProgress de $totalItems ($percentage%)"

        Log.d(TAG, message)
        _syncProgress.emit(SyncProgress(index + 1, totalItems, message))
        notificationManager.updateSyncProgress(index + 1, totalItems, notificationMessage)
    }

    private suspend fun syncEvaluacionGeneral(queueItem: SyncQueueEntity) {
        val eval = evaluacionGeneralDao.getEvaluacionGeneralById(queueItem.entityId)
            ?: throw IllegalStateException("EvaluacionGeneral ID ${queueItem.entityId} no encontrada")
        Log.d(TAG, "Sincronizando EvaluacionGeneral ID ${eval.id}, serverId: ${eval.serverId}")

        // Verificar si ya está sincronizado
        if (eval.serverId != null && eval.syncStatus == "SYNCED") {
            withContext(Dispatchers.IO) {
                syncQueueDao.transaction {
                    syncQueueDao.dequeue("EvaluacionGeneral", eval.id)
                    Log.d(TAG, "✅ EvaluacionGeneral ID ${eval.id} ya sincronizada, eliminando de la cola")
                }
            }
            return
        }

        // Verificar si está en proceso de sincronización
        if (eval.syncStatus == "SYNCING") {
            Log.w(TAG, "⚠️ EvaluacionGeneral ID ${eval.id} ya en proceso de sincronización, saltando")
            return
        }

        // Verificar si el registro ya existe en el servidor (por campos únicos)
        if (eval.serverId == null) {
            try {
                val existingResponse = evaluacionGeneralApiService.getEvaluacionesGenerales()
                if (existingResponse.isSuccessful && existingResponse.body() != null) {
                    val existingEval = existingResponse.body()!!.find { serverEval ->
                        serverEval.fecha == eval.fecha &&
                                serverEval.hora == eval.hora &&
                                serverEval.idevaluadorev == eval.idevaluadorev &&
                                serverEval.idpolinizadorev == eval.idpolinizadorev &&
                                serverEval.idloteev == eval.idLoteev
                    }
                    if (existingEval != null) {
                        withContext(Dispatchers.IO) {
                            syncQueueDao.transaction {
                                val updatedEval = eval.copy(
                                    serverId = existingEval.id,
                                    syncStatus = "SYNCED",
                                    timestamp = existingEval.timestamp
                                )
                                evaluacionGeneralDao.updateEvaluacionGeneral(updatedEval)
                                syncQueueDao.dequeue("EvaluacionGeneral", eval.id)
                                Log.d(TAG, "✅ EvaluacionGeneral ID ${eval.id} ya existe en el servidor como serverId ${existingEval.id}, actualizada localmente")
                            }
                        }
                        return
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ No se pudo verificar existencia en servidor para EvaluacionGeneral ID ${eval.id}: ${e.message}")
            }
        }

        // Verificar conflicto con el servidor
        if (eval.serverId != null) {
            try {
                val serverResponse = evaluacionGeneralApiService.getEvaluacionGeneralById(eval.serverId!!)
                if (serverResponse.isSuccessful && serverResponse.body() != null) {
                    val serverEval = serverResponse.body()!!
                    if (serverEval.timestamp > eval.timestamp) {
                        withContext(Dispatchers.IO) {
                            syncQueueDao.transaction {
                                val mergedEval = mergeEvaluacionGeneral(eval, serverEval)
                                evaluacionGeneralDao.updateEvaluacionGeneral(mergedEval)
                                syncQueueDao.dequeue("EvaluacionGeneral", eval.id)
                            }
                        }
                        Log.d(TAG, "✅ EvaluacionGeneral ID ${eval.id} actualizada desde el servidor")
                        return
                    }
                } else {
                    Log.w(TAG, "⚠️ Respuesta no exitosa al verificar EvaluacionGeneral serverId ${eval.serverId}: ${serverResponse.code()}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ No se pudo verificar conflicto para EvaluacionGeneral ID ${eval.id}: ${e.message}")
            }
        }

        // Actualizar estado a SYNCING
        val updatedEval = eval.copy(syncStatus = "SYNCING")
        withContext(Dispatchers.IO) {
            evaluacionGeneralDao.transaction {
                evaluacionGeneralDao.updateEvaluacionGeneral(updatedEval)
            }
        }

        var lastException: Exception? = null
        for (attempt in 1..MAX_RETRIES) {
            try {
                val domainEval = EvaluacionGeneralMapper.toDomain(updatedEval)
                val request = EvaluacionGeneralMapper.toRequest(domainEval)
                val response = if (eval.serverId != null) {
                    evaluacionGeneralApiService.updateEvaluacionGeneral(eval.serverId!!, request)
                } else {
                    evaluacionGeneralApiService.createEvaluacionGeneral(request)
                }

                if (response.isSuccessful && response.body() != null) {
                    val serverEval = response.body()!!
                    // Subir foto y firma si existen y recolectar URLs
                    var photoUrl: String? = serverEval.fotopath ?: domainEval.fotoPath
                    var signatureUrl: String? = serverEval.firmapath ?: domainEval.firmaPath

                    if (eval.fotoPath != null && !eval.fotoPath!!.startsWith("http")) {
                        try {
                            photoUrl = uploadPhotoToServer(serverEval.id, eval.fotoPath!!)
                            Log.d(TAG, "✅ Foto subida para EvaluacionGeneral ID ${eval.id}, URL: $photoUrl")
                        } catch (e: Exception) {
                            Log.w(TAG, "⚠️ Fallo al subir foto para EvaluacionGeneral ID ${eval.id}: ${e.message}")
                        }
                    }
                    if (eval.firmaPath != null && !eval.firmaPath!!.startsWith("http")) {
                        try {
                            signatureUrl = uploadSignatureToServer(serverEval.id, eval.firmaPath!!)
                            Log.d(TAG, "✅ Firma subida para EvaluacionGeneral ID ${eval.id}, URL: $signatureUrl")
                        } catch (e: Exception) {
                            Log.w(TAG, "⚠️ Fallo al subir firma para EvaluacionGeneral ID ${eval.id}: ${e.message}")
                        }
                    }

                    // Actualizar localmente en una transacción
                    withContext(Dispatchers.IO) {
                        evaluacionGeneralDao.transaction {
                            val syncedEval = domainEval.copy(
                                serverId = serverEval.id,
                                syncStatus = "SYNCED",
                                timestamp = serverEval.timestamp,
                                fotoPath = photoUrl,
                                firmaPath = signatureUrl
                            )
                            evaluacionGeneralDao.updateEvaluacionGeneral(EvaluacionGeneralMapper.toDatabase(syncedEval))
                            syncQueueDao.dequeue("EvaluacionGeneral", eval.id)
                            Log.d(TAG, "✅ Sincronizada EvaluacionGeneral ID ${eval.id} con serverId ${serverEval.id}, fotoPath: $photoUrl, firmaPath: $signatureUrl")
                        }
                    }
                    return
                } else {
                    throw Exception("Error del servidor: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "⚠️ Intento $attempt fallido para EvaluacionGeneral ID ${eval.id}: ${e.message}")
                if (attempt < MAX_RETRIES) {
                    delay(RETRY_DELAY_MS)
                }
            }
        }

        // Si todos los intentos fallan, marcar como FAILED
        withContext(Dispatchers.IO) {
            evaluacionGeneralDao.transaction {
                val updatedFailedEval = eval.copy(syncStatus = "FAILED")
                evaluacionGeneralDao.updateEvaluacionGeneral(updatedFailedEval)
            }
        }
        throw lastException ?: Exception("Fallo al sincronizar EvaluacionGeneral ID ${eval.id} tras $MAX_RETRIES intentos")
    }

    private suspend fun syncEvaluacionPolinizacion(queueItem: SyncQueueEntity) {
        val eval = evaluacionPolinizacionDao.getEvaluacionById(queueItem.entityId)
            ?: throw IllegalStateException("EvaluacionPolinizacion ID ${queueItem.entityId} no encontrada")
        Log.d(TAG, "Sincronizando EvaluacionPolinizacion ID ${eval.id}, serverId: ${eval.serverId}, evaluacionGeneralId: ${eval.evaluacionGeneralId}")

        // Verificar si ya está sincronizado
        if (eval.serverId != null && eval.syncStatus == "SYNCED") {
            withContext(Dispatchers.IO) {
                syncQueueDao.transaction {
                    syncQueueDao.dequeue("EvaluacionPolinizacion", eval.id)
                    Log.d(TAG, "✅ EvaluacionPolinizacion ID ${eval.id} ya sincronizada, eliminada de la cola")
                }
            }
            return
        }

        // Verificar si está en proceso de sincronización
        if (eval.syncStatus == "SYNCING") {
            Log.w(TAG, "⚠️ EvaluacionPolinizacion ID ${eval.id} ya en proceso de sincronización, saltando")
            return
        }

        // Verificar que la EvaluacionGeneral esté sincronizada
        var generalServerIdToUse: Int? = null
        val localGeneralId = eval.evaluacionGeneralId
        if (localGeneralId != null) {
            val generalEval = evaluacionGeneralDao.getEvaluacionGeneralById(localGeneralId)
            if (generalEval == null || generalEval.syncStatus != "SYNCED" || generalEval.serverId == null) {
                Log.w(TAG, "⚠️ EvaluacionGeneral ID $localGeneralId no sincronizada, encolando y postponiendo")
                withContext(Dispatchers.IO) {
                    syncQueueDao.transaction {
                        val existingQueueItem = syncQueueDao.getQueueItem("EvaluacionGeneral", localGeneralId)
                        if (generalEval != null && existingQueueItem == null) {
                            syncQueueDao.enqueue(SyncQueueEntity(entityType = "EvaluacionGeneral", entityId = localGeneralId))
                            Log.d(TAG, "📋 Encolada EvaluacionGeneral ID $localGeneralId")
                        }
                    }
                }
                handleSyncFailure(queueItem)
                return
            }
            generalServerIdToUse = generalEval.serverId
            Log.d(TAG, "Usando ID del servidor para EvaluacionGeneral: $generalServerIdToUse")
        } else {
            Log.w(TAG, "⚠️ EvaluacionPolinizacion ID ${eval.id} no tiene evaluacionGeneralId")
            handleSyncFailure(queueItem)
            return
        }

        // Verificar si el registro ya existe en el servidor con reintentos
        var existingEval: EvaluacionResponse? = null
        var lastVerificationException: Exception? = null
        val maxVerificationRetries = 3
        val verificationRetryDelayMs = 5000L

        for (attempt in 1..maxVerificationRetries) {
            try {
                val existingResponse = evaluacionApiService.getEvaluaciones()
                if (existingResponse.isSuccessful && existingResponse.body() != null) {
                    existingEval = existingResponse.body()!!.find { serverEval ->
                        serverEval.evaluaciongeneralid == generalServerIdToUse &&
                                serverEval.fecha == eval.fecha &&
                                serverEval.hora == eval.hora &&
                                serverEval.idevaluador == eval.idEvaluador &&
                                serverEval.idpolinizador == eval.idPolinizador &&
                                serverEval.idlote == eval.idlote &&
                                serverEval.seccion == eval.seccion &&
                                serverEval.palma == eval.palma
                    }
                    if (existingEval != null) {
                        withContext(Dispatchers.IO) {
                            syncQueueDao.transaction {
                                val updatedEval = eval.copy(
                                    serverId = existingEval!!.id,
                                    syncStatus = "SYNCED",
                                    timestamp = existingEval!!.timestamp
                                )
                                evaluacionPolinizacionDao.updateEvaluacion(updatedEval)
                                syncQueueDao.dequeue("EvaluacionPolinizacion", eval.id)
                                Log.d(TAG, "✅ EvaluacionPolinizacion ID ${eval.id} ya existe en el servidor como serverId ${existingEval!!.id}, actualizada localmente")
                            }
                        }
                        return
                    }
                    // Si no se encuentra el registro, salir del bucle de reintentos
                    break
                } else {
                    throw Exception("Respuesta no exitosa al verificar EvaluacionPolinizacion: ${existingResponse.code()} - ${existingResponse.message()}")
                }
            } catch (e: Exception) {
                lastVerificationException = e
                Log.w(TAG, "⚠️ Intento $attempt fallido al verificar existencia en servidor para EvaluacionPolinizacion ID ${eval.id}: ${e.message}")
                if (attempt < maxVerificationRetries) {
                    delay(verificationRetryDelayMs)
                }
            }
        }

        if (lastVerificationException != null && existingEval == null) {
            Log.w(TAG, "⚠️ No se pudo verificar existencia en servidor para EvaluacionPolinizacion ID ${eval.id} tras $maxVerificationRetries intentos: ${lastVerificationException.message}")
            // Continuar con la sincronización, pero registrar el fallo
        }

        // Verificar conflicto con el servidor si hay serverId
        if (eval.serverId != null) {
            try {
                val serverResponse = evaluacionApiService.getEvaluacionById(eval.serverId!!)
                if (serverResponse.isSuccessful && serverResponse.body() != null) {
                    val serverEval = serverResponse.body()!!
                    if (serverEval.timestamp > eval.timestamp) {
                        withContext(Dispatchers.IO) {
                            syncQueueDao.transaction {
                                val mergedEval = mergeEvaluacionPolinizacion(eval, serverEval).copy(evaluacionGeneralId = localGeneralId)
                                evaluacionPolinizacionDao.updateEvaluacion(mergedEval)
                                syncQueueDao.dequeue("EvaluacionPolinizacion", eval.id)
                            }
                        }
                        Log.d(TAG, "✅ EvaluacionPolinizacion ID ${eval.id} actualizada desde el servidor")
                        return
                    }
                } else {
                    Log.w(TAG, "⚠️ Respuesta no exitosa al verificar EvaluacionPolinizacion serverId ${eval.serverId}: ${serverResponse.code()}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ No se pudo verificar conflicto para EvaluacionPolinizacion ID ${eval.id}: ${e.message}")
            }
        }

        // Actualizar estado a SYNCING
        val updatedEval = eval.copy(syncStatus = "SYNCING")
        try {
            withContext(Dispatchers.IO) {
                evaluacionPolinizacionDao.transaction {
                    evaluacionPolinizacionDao.updateEvaluacion(updatedEval)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al actualizar estado SYNCING para EvaluacionPolinizacion ID ${eval.id}: ${e.message}")
            handleSyncFailure(queueItem)
            return
        }

        // Intentar sincronización con reintentos
        var lastException: Exception? = null
        for (attempt in 1..MAX_RETRIES) {
            try {
                // Verificar nuevamente la existencia antes de cada intento
                val existingResponse = evaluacionApiService.getEvaluaciones()
                if (existingResponse.isSuccessful && existingResponse.body() != null) {
                    val recheckedEval = existingResponse.body()!!.find { serverEval ->
                        serverEval.evaluaciongeneralid == generalServerIdToUse &&
                                serverEval.fecha == eval.fecha &&
                                serverEval.hora == eval.hora &&
                                serverEval.idevaluador == eval.idEvaluador &&
                                serverEval.idpolinizador == eval.idPolinizador &&
                                serverEval.idlote == eval.idlote &&
                                serverEval.seccion == eval.seccion &&
                                serverEval.palma == eval.palma
                    }
                    if (recheckedEval != null) {
                        withContext(Dispatchers.IO) {
                            syncQueueDao.transaction {
                                val updatedEval = eval.copy(
                                    serverId = recheckedEval.id,
                                    syncStatus = "SYNCED",
                                    timestamp = recheckedEval.timestamp
                                )
                                evaluacionPolinizacionDao.updateEvaluacion(updatedEval)
                                syncQueueDao.dequeue("EvaluacionPolinizacion", eval.id)
                                Log.d(TAG, "✅ EvaluacionPolinizacion ID ${eval.id} ya existe en el servidor como serverId ${recheckedEval.id}, actualizada localmente")
                            }
                        }
                        return
                    }
                }

                val domainEval = EvaluacionPolinizacionMapper.toDomain(updatedEval)
                val domainEvalWithServerIds = domainEval.copy(evaluacionGeneralId = generalServerIdToUse)
                val request = EvaluacionPolinizacionMapper.toRequest(domainEvalWithServerIds)
                Log.d(TAG, "Enviando request para EvaluacionPolinizacion ID ${eval.id} con evaluacionGeneralId: ${request.evaluaciongeneralid}")

                val response = if (eval.serverId != null) {
                    evaluacionApiService.updateEvaluacion(eval.serverId!!, request)
                } else {
                    evaluacionApiService.createEvaluacion(request)
                }

                if (response.isSuccessful && response.body() != null) {
                    val serverEval = response.body()!!
                    withContext(Dispatchers.IO) {
                        evaluacionPolinizacionDao.transaction {
                            val syncedEval = domainEval.copy(
                                serverId = serverEval.id,
                                syncStatus = "SYNCED",
                                timestamp = serverEval.timestamp,
                                evaluacionGeneralId = localGeneralId
                            )
                            evaluacionPolinizacionDao.updateEvaluacion(EvaluacionPolinizacionMapper.toDatabase(syncedEval))
                            syncQueueDao.dequeue("EvaluacionPolinizacion", eval.id)
                            Log.d(TAG, "✅ Sincronizada EvaluacionPolinizacion ID ${eval.id} con serverId ${serverEval.id}")
                        }
                    }
                    return
                } else {
                    throw Exception("Error del servidor: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "⚠️ Intento $attempt fallido para EvaluacionPolinizacion ID ${eval.id}: ${e.message}")
                if (e.message?.contains("FOREIGN KEY constraint failed") == true) {
                    Log.e(TAG, "❌ Error de clave foránea, postponiendo sincronización")
                    handleSyncFailure(queueItem)
                    return
                }
                if (attempt < MAX_RETRIES) {
                    delay(RETRY_DELAY_MS)
                }
            }
        }

        // Si todos los intentos fallan, marcar como FAILED
        withContext(Dispatchers.IO) {
            evaluacionPolinizacionDao.transaction {
                val updatedFailedEval = eval.copy(syncStatus = "FAILED")
                evaluacionPolinizacionDao.updateEvaluacion(updatedFailedEval)
            }
        }
        throw lastException ?: Exception("Fallo al sincronizar EvaluacionPolinizacion ID ${eval.id} tras $MAX_RETRIES intentos")
    }

    private suspend fun uploadPhotoToServer(evaluacionId: Int, photoPath: String): String? {
        val file = File(photoPath)
        if (!file.exists()) {
            Log.w(TAG, "⚠️ Foto no encontrada en $photoPath para EvaluacionGeneral ID $evaluacionId")
            return null
        }

        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val photoPart = MultipartBody.Part.createFormData("photo", file.name, requestFile)

        try {
            val response = evaluacionGeneralApiService.uploadPhoto(evaluacionId, photoPart)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.containsKey("url")) {
                    val url = body["url"]
                    Log.d(TAG, "✅ Foto subida para EvaluacionGeneral ID $evaluacionId: $url")
                    return url
                }
            }
            Log.e(TAG, "❌ Error al subir foto para EvaluacionGeneral ID $evaluacionId: ${response.code()} - ${response.message()}")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al subir foto para EvaluacionGeneral ID $evaluacionId: ${e.message}", e)
            return null
        }
    }

    private suspend fun uploadSignatureToServer(evaluacionId: Int, signaturePath: String): String? {
        val file = File(signaturePath)
        if (!file.exists()) {
            Log.w(TAG, "⚠️ Firma no encontrada en $signaturePath para EvaluacionGeneral ID $evaluacionId")
            return null
        }

        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val signaturePart = MultipartBody.Part.createFormData("signature", file.name, requestFile)

        try {
            val response = evaluacionGeneralApiService.uploadSignature(evaluacionId, signaturePart)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.containsKey("url")) {
                    val url = body["url"]
                    Log.d(TAG, "✅ Firma subida para EvaluacionGeneral ID $evaluacionId: $url")
                    return url
                }
            }
            Log.e(TAG, "❌ Error al subir firma para EvaluacionGeneral ID $evaluacionId: ${response.code()} - ${response.message()}")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al subir firma para EvaluacionGeneral ID $evaluacionId: ${e.message}", e)
            return null
        }
    }

    private suspend fun handleSyncFailure(queueItem: SyncQueueEntity) {
        val updatedItem = queueItem.copy(retryCount = queueItem.retryCount + 1)
        if (updatedItem.retryCount >= MAX_RETRIES) {
            syncQueueDao.dequeue(queueItem.entityType, queueItem.entityId)
            Log.w(TAG, "🚫 Máximo de reintentos alcanzado para ${queueItem.entityType} ID ${queueItem.entityId}")
            notificationManager.updateSyncMessage("Error: Máximo de reintentos alcanzado para ${queueItem.entityType} ID ${queueItem.entityId}")
        } else {
            syncQueueDao.update(updatedItem)
            delay(RETRY_DELAY_MS)
            Log.d(TAG, "🔄 Reintentando ${queueItem.entityType} ID ${queueItem.entityId} (Intento ${updatedItem.retryCount + 1})")
        }
    }

    fun scheduleBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork("sync_work", ExistingPeriodicWorkPolicy.KEEP, syncRequest)
    }

    private fun mergeEvaluacionGeneral(local: EvaluacionGeneralEntity, server: EvaluacionGeneralResponse): EvaluacionGeneralEntity {
        return local.copy(
            serverId = server.id,
            fecha = server.fecha,
            hora = server.hora,
            semana = server.semana,
            idevaluadorev = server.idevaluadorev,
            idpolinizadorev = server.idpolinizadorev,
            idLoteev = server.idloteev,
            syncStatus = "SYNCED",
            timestamp = server.timestamp,
            fotoPath = server.fotopath ?: local.fotoPath,
            firmaPath = server.firmapath ?: local.firmaPath
        )
    }

    private fun mergeEvaluacionPolinizacion(local: EvaluacionPolinizacionEntity, server: EvaluacionResponse): EvaluacionPolinizacionEntity {
        return local.copy(
            serverId = server.id,
            fecha = server.fecha ?: local.fecha,
            hora = server.hora ?: local.hora,
            semana = server.semana,
            ubicacion = server.ubicacion ?: local.ubicacion,
            idEvaluador = server.idevaluador,
            idPolinizador = server.idpolinizador,
            idlote = server.idlote,
            seccion = server.seccion,
            palma = server.palma ?: local.palma,
            inflorescencia = server.inflorescencia ?: local.inflorescencia,
            antesis = server.antesis ?: local.antesis,
            antesisDejadas = server.antesisDejadas ?: local.antesisDejadas,
            postantesisDejadas = server.postantesisDejadas ?: local.postantesisDejadas,
            postantesis = server.postantesis ?: local.postantesis,
            espate = server.espate ?: local.espate,
            aplicacion = server.aplicacion ?: local.aplicacion,
            marcacion = server.marcacion ?: local.marcacion,
            repaso1 = server.repaso1 ?: local.repaso1,
            repaso2 = server.repaso2 ?: local.repaso2,
            observaciones = server.observaciones ?: local.observaciones,
            syncStatus = "SYNCED",
            evaluacionGeneralId = local.evaluacionGeneralId, // Mantener el ID local
            timestamp = server.timestamp
        )
    }
}