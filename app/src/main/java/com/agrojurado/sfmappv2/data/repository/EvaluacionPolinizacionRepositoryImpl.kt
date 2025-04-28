package com.agrojurado.sfmappv2.data.repository

import android.content.Context
import android.util.Log
import com.agrojurado.sfmappv2.data.local.dao.EvaluacionGeneralDao
import com.agrojurado.sfmappv2.data.local.dao.EvaluacionPolinizacionDao
import com.agrojurado.sfmappv2.data.mapper.EvaluacionPolinizacionMapper
import com.agrojurado.sfmappv2.data.remote.api.EvaluacionApiService
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.domain.repository.EvaluacionPolinizacionRepository
import com.agrojurado.sfmappv2.domain.repository.OperarioRepository
import com.agrojurado.sfmappv2.domain.repository.UsuarioRepository
import com.agrojurado.sfmappv2.domain.security.UserRoleConstants
import com.agrojurado.sfmappv2.data.remote.dto.common.utils.Utils
import com.agrojurado.sfmappv2.data.remote.dto.evaluacion.EvaluacionResponse
import com.agrojurado.sfmappv2.utils.SyncNotificationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

class EvaluacionPolinizacionRepositoryImpl @Inject constructor(
    private val dao: EvaluacionPolinizacionDao,
    private val apiService: EvaluacionApiService,
    private val evaluacionGeneralDao: EvaluacionGeneralDao,
    private val usuarioRepository: UsuarioRepository,
    private val operarioRepository: OperarioRepository,
    @ApplicationContext private val context: Context
) : EvaluacionPolinizacionRepository {

    companion object {
        private const val TAG = "EvaluacionRepository"
    }

    private fun isNetworkAvailable(): Boolean = Utils.isNetworkAvailable(context)

    private suspend fun notifyUser(message: String) {
        withContext(Dispatchers.Main) {
            Utils.showToast(context, message)
        }
    }

    override fun getEvaluaciones(): Flow<List<EvaluacionPolinizacion>> {
        return dao.getEvaluaciones()
            .map { entities -> entities.map(EvaluacionPolinizacionMapper::toDomain) }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getEvaluacionById(id: Int): EvaluacionPolinizacion? {
        return dao.getEvaluacionById(id)?.let(EvaluacionPolinizacionMapper::toDomain)
    }

    override suspend fun getLastEvaluacion(): EvaluacionPolinizacion? {
        return dao.getLastEvaluacion()?.let(EvaluacionPolinizacionMapper::toDomain)
    }

    override suspend fun checkPalmExists(semana: Int, lote: Int, palma: Int, idPolinizador: Int, seccion: Int, evaluacionGeneralId: Int): Boolean {
        return dao.checkPalmExists(semana, lote, palma, idPolinizador, seccion, evaluacionGeneralId) > 0
    }

    override suspend fun insertEvaluacion(evaluacion: EvaluacionPolinizacion): Long {
        return withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            val localId = dao.insertEvaluacion(
                EvaluacionPolinizacionMapper.toDatabase(
                    evaluacion.copy(
                        isSynced = false,
                        timestamp = timestamp
                    )
                )
            )
            notifyUser("Evaluación de polinización guardada localmente")
            localId
        }
    }

    override suspend fun updateEvaluacion(evaluacion: EvaluacionPolinizacion) {
        withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            val updatedEntity = EvaluacionPolinizacionMapper.toDatabase(evaluacion).copy(
                isSynced = false,
                timestamp = timestamp
            )
            dao.updateEvaluacion(updatedEntity)
            notifyUser("Actualización guardada localmente")
        }
    }

    override suspend fun deleteEvaluacion(evaluacion: EvaluacionPolinizacion) {
        withContext(Dispatchers.IO) {
            dao.deleteEvaluacion(EvaluacionPolinizacionMapper.toDatabase(evaluacion))
            notifyUser("Eliminada localmente")
        }
    }

    override suspend fun deleteEvaluacionesByEvaluacionGeneralId(evaluacionGeneralId: Int) {
        withContext(Dispatchers.IO) {
            dao.deleteByEvaluacionGeneralId(evaluacionGeneralId)
            Log.d(TAG, "🗑️ Deleted EvaluacionPolinizacion with evaluacionGeneralId: $evaluacionGeneralId")
        }
    }

    override suspend fun asociarConEvaluacionGeneral(evaluacionId: Int, evaluacionGeneralId: Int) {
        withContext(Dispatchers.IO) {
            dao.updateEvaluacionGeneralId(evaluacionId, evaluacionGeneralId)
            Log.d(TAG, "Asociada EvaluacionPolinizacion ID $evaluacionId con EvaluacionGeneral ID $evaluacionGeneralId")
        }
    }

    override fun getEvaluacionesByEvaluacionGeneralId(evaluacionGeneralId: Int): Flow<List<EvaluacionPolinizacion>> {
        return dao.getEvaluacionesByEvaluacionGeneralId(evaluacionGeneralId)
            .map { entities -> entities.map(EvaluacionPolinizacionMapper::toDomain) }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun syncEvaluacionesForGeneral(evaluaciones: List<EvaluacionPolinizacion>, serverGeneralId: Int) {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Sin conexión, sincronización de EvaluacionPolinizacion pendiente")
            notifyUser("⚠️ Sin conexión, sincronización pendiente")
            return
        }

        if (serverGeneralId <= 0) {
            Log.e(TAG, "❌ Error: ID de servidor de evaluación general inválido ($serverGeneralId)")
            notifyUser("⚠️ Error de sincronización: ID de evaluación general inválido")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 Iniciando sincronización de ${evaluaciones.size} evaluaciones para EvaluacionGeneral ID del servidor: $serverGeneralId")

                if (evaluaciones.isEmpty()) {
                    Log.d(TAG, "✅ No hay evaluaciones para sincronizar")
                    return@withContext
                }

                val notificationManager = SyncNotificationManager.getInstance(context)
                notificationManager.updateSyncMessage("Sincronizando ${evaluaciones.size} eventos individuales")

                val pendientes = evaluaciones.filter { !it.isSynced || it.serverId == null }
                val yaExistentes = evaluaciones.filter { it.isSynced && it.serverId != null }

                Log.d(TAG, "📊 Evaluaciones pendientes: ${pendientes.size}, ya existentes: ${yaExistentes.size}")

                notificationManager.updateSyncMessage("Sincronizando ${pendientes.size} eventos pendientes")

                // Ajustar BATCH_SIZE según la velocidad de la red
                val BATCH_SIZE = when (Utils.getNetworkSpeed(context)) {
                    Utils.NetworkSpeed.FAST -> 10
                    Utils.NetworkSpeed.MEDIUM -> 5
                    Utils.NetworkSpeed.SLOW -> 3
                    Utils.NetworkSpeed.NONE -> 3 // Preparar para próximo intento
                }
                Log.d(TAG, "📦 Usando tamaño de lote: $BATCH_SIZE (red: ${Utils.getNetworkSpeed(context)})")

                val batches = pendientes.chunked(BATCH_SIZE)

                val syncedIds = mutableListOf<Int>()
                val failedIds = mutableListOf<Int>()

                if (batches.size > 1) {
                    Log.d(TAG, "📦 Procesando en ${batches.size} lotes de máximo $BATCH_SIZE items")
                    notificationManager.updateSyncMessage("Procesando en ${batches.size} lotes")
                }

                val totalItems = pendientes.size
                var currentItem = 0

                batches.forEachIndexed { batchIndex, batch ->
                    try {
                        notificationManager.updateSyncProgress(
                            currentItem,
                            totalItems,
                            "Lote ${batchIndex+1}/${batches.size}: ${batch.size} eventos"
                        )

                        val batchRequests = batch.map { localEval ->
                            EvaluacionPolinizacionMapper.toRequest(localEval).copy(
                                evaluaciongeneralid = serverGeneralId
                            )
                        }

                        Log.d(TAG, "📤 Enviando lote ${batchIndex+1}/${batches.size} (${batch.size} items)")

                        if (batch.size > 1) {
                            val batchResponse = apiService.syncEvaluaciones(batchRequests)

                            if (batchResponse.isSuccessful && batchResponse.body() != null) {
                                val serverResults = batchResponse.body()!!

                                if (serverResults.size != batch.size) {
                                    Log.w(TAG, "⚠️ Respuesta del servidor contiene ${serverResults.size} elementos, pero se enviaron ${batch.size}")
                                }

                                val matchedResults = serverResults.zip(batch)
                                matchedResults.forEachIndexed { itemIndex, (serverEval, localEval) ->
                                    try {
                                        if (serverEval.id <= 0) {
                                            Log.e(TAG, "❌ ID inválido para evaluación ${localEval.id}")
                                            failedIds.add(localEval.id!!)
                                        } else {
                                            processSyncResult(localEval, serverEval, syncedIds, failedIds)
                                        }

                                        currentItem++
                                        val progress = (currentItem * 100) / totalItems
                                        if (itemIndex % 3 == 0 || itemIndex == matchedResults.size - 1) {
                                            notificationManager.updateSyncProgress(
                                                currentItem,
                                                totalItems,
                                                "Progreso: $progress% (${currentItem}/${totalItems})"
                                            )
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "❌ Error procesando resultado para ID ${localEval.id}: ${e.message}", e)
                                        failedIds.add(localEval.id!!)
                                    }
                                }

                                // Reintentar registros no incluidos en la respuesta
                                val missingIds = batch.filter { eval -> !syncedIds.contains(eval.id) && !failedIds.contains(eval.id) }
                                if (missingIds.isNotEmpty()) {
                                    Log.w(TAG, "⚠️ ${missingIds.size} registros no incluidos en respuesta, reintentando individualmente")
                                    processIndividually(missingIds, serverGeneralId, syncedIds, failedIds, totalItems, currentItem, notificationManager)
                                    currentItem += missingIds.size
                                }

                                Log.d(TAG, "✅ Lote ${batchIndex+1} procesado: ${matchedResults.size} items")
                            } else {
                                Log.w(TAG, "⚠️ Falló sincronización por lotes (${batchResponse.code()}), procesando individualmente")
                                processIndividually(batch, serverGeneralId, syncedIds, failedIds, totalItems, currentItem, notificationManager)
                                currentItem += batch.size
                            }
                        } else {
                            processIndividually(batch, serverGeneralId, syncedIds, failedIds, totalItems, currentItem, notificationManager)
                            currentItem += batch.size
                        }

                        if (batchIndex < batches.size - 1) {
                            // Ajustar pausa según velocidad de red
                            val delayMs = if (Utils.getNetworkSpeed(context) == Utils.NetworkSpeed.FAST) 200L else 500L
                            kotlinx.coroutines.delay(delayMs)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error general en lote ${batchIndex+1}: ${e.message}", e)
                        batch.forEach { localEval ->
                            if (localEval.id != null && !failedIds.contains(localEval.id)) {
                                failedIds.add(localEval.id!!)
                            }
                        }
                        currentItem += batch.size
                    }
                }

                if (failedIds.isEmpty()) {
                    Log.d(TAG, "✅ Todas las evaluaciones (${pendientes.size}) sincronizadas correctamente")
                    notificationManager.updateSyncMessage("✅ ${pendientes.size} eventos sincronizados")
                    notifyUser("✅ ${pendientes.size} evaluaciones sincronizadas")
                } else {
                    val message = "${syncedIds.size}/${pendientes.size} sincronizadas, ${failedIds.size} fallidas"
                    Log.w(TAG, "⚠️ $message")
                    notificationManager.updateSyncMessage("⚠️ $message")
                    notifyUser("⚠️ $message (${failedIds.size} fallidas)")

                    if (failedIds.isNotEmpty()) {
                        val failedEvaluaciones = pendientes.filter { it.id != null && failedIds.contains(it.id) }
                        retryFailedEvaluaciones(failedEvaluaciones, serverGeneralId, notificationManager)
                    }
                }

                try {
                    fetchEvaluacionesFromServer()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error al actualizar desde servidor: ${e.message}", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error general en sincronización: ${e.message}", e)
                notifyUser("⚠️ Error de sincronización: ${e.message}")
            }
        }
    }

    // Método auxiliar para procesar evaluaciones individualmente
    private suspend fun processIndividually(
        evaluaciones: List<EvaluacionPolinizacion>,
        serverGeneralId: Int,
        syncedIds: MutableList<Int>,
        failedIds: MutableList<Int>,
        totalItems: Int,
        currentItem: Int,
        notificationManager: SyncNotificationManager
    ) {
        evaluaciones.forEach { localEval ->
            try {
                syncIndividualEvaluacion(localEval, serverGeneralId, syncedIds, failedIds, totalItems, currentItem, notificationManager)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error procesando individualmente ID ${localEval.id}: ${e.message}", e)
                if (localEval.id != null && !failedIds.contains(localEval.id)) {
                    failedIds.add(localEval.id!!)
                }
            }
        }
    }

    private suspend fun processSyncResult(
        localEvaluacion: EvaluacionPolinizacion,
        serverEval: EvaluacionResponse,
        syncedIds: MutableList<Int>,
        failedIds: MutableList<Int>
    ) {
        try {
            if (serverEval.id <= 0) {
                Log.e(TAG, "⚠️ ID de servidor inválido (${serverEval.id}) para evaluación ${localEvaluacion.id}")
                failedIds.add(localEvaluacion.id!!)
                return
            }

            val syncedEvaluacion = localEvaluacion.copy(
                serverId = serverEval.id,
                isSynced = true,
                timestamp = serverEval.timestamp
            )
            dao.updateEvaluacion(EvaluacionPolinizacionMapper.toDatabase(syncedEvaluacion))
            Log.d(TAG, "✅ Sincronizada EvaluacionPolinizacion ID ${localEvaluacion.id} con serverId ${serverEval.id}")
            syncedIds.add(localEvaluacion.id!!)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error procesando resultado: ${e.message}", e)
            failedIds.add(localEvaluacion.id!!)
        }
    }

    private suspend fun syncIndividualEvaluacion(
        localEvaluacion: EvaluacionPolinizacion, 
        serverGeneralId: Int,
        syncedIds: MutableList<Int>,
        failedIds: MutableList<Int>,
        totalItems: Int,
        currentProgress: Int,
        notificationManager: SyncNotificationManager
    ) {
        try {
            // Verificación adicional de serverGeneralId
            if (serverGeneralId <= 0) {
                Log.e(TAG, "❌ Error: ID de servidor de evaluación general inválido ($serverGeneralId) para evaluación ${localEvaluacion.id}")
                failedIds.add(localEvaluacion.id!!)
                return
            }
            
            // Actualizar progreso en la notificación
            val progress = ((currentProgress + 1) * 100) / totalItems
            notificationManager.updateSyncProgress(
                currentProgress + 1,
                totalItems,
                "Sincronizando evento ${currentProgress + 1}/${totalItems} (${localEvaluacion.id})"
            )
            
            // Marcar como no sincronizado para asegurar actualización
            val updatedLocalEvaluacion = localEvaluacion.copy(
                isSynced = false
            )

            // Actualizar localmente para mantener el estado correcto
            dao.updateEvaluacion(EvaluacionPolinizacionMapper.toDatabase(updatedLocalEvaluacion))

            // Preparar solicitud con el ID de servidor correcto para evaluación general
            val evaluacionRequest = EvaluacionPolinizacionMapper.toRequest(updatedLocalEvaluacion).copy(
                evaluaciongeneralid = serverGeneralId
            )
            
            Log.d(TAG, "📤 Enviando evaluación ${localEvaluacion.id} con evaluacionGeneralId=$serverGeneralId al servidor")
            
            // Enviar al servidor (crear o actualizar)
            val response = if (localEvaluacion.serverId != null) {
                apiService.updateEvaluacion(localEvaluacion.serverId!!, evaluacionRequest)
            } else {
                apiService.createEvaluacion(evaluacionRequest)
            }

            // Procesar respuesta
            if (response.isSuccessful && response.body() != null) {
                val serverEvaluacion = response.body()!!
                processSyncResult(localEvaluacion, serverEvaluacion, syncedIds, failedIds)
                notificationManager.updateSyncMessage("Evento ${localEvaluacion.id} sincronizado correctamente")
            } else {
                Log.e(TAG, "❌ Error de servidor para ID ${localEvaluacion.id}: ${response.code()} - ${response.errorBody()?.string()}")
                failedIds.add(localEvaluacion.id!!)
                notificationManager.updateSyncMessage("Error al sincronizar evento ${localEvaluacion.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Excepción sincronizando ID ${localEvaluacion.id}: ${e.message}", e)
            failedIds.add(localEvaluacion.id!!)
            notificationManager.updateSyncMessage("Error al sincronizar evento ${localEvaluacion.id}: ${e.message}")
        }
    }

    override suspend fun fetchEvaluacionesFromServer() {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "No network available, skipping fetch from server")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getEvaluaciones() // Fetch EvaluacionPolinizacion from server
                if (response.isSuccessful) {
                    response.body()?.let { serverEvaluaciones ->
                        // Obtener el usuario actual
                        val currentUser = usuarioRepository.getLoggedInUserEmail()?.let { email ->
                            usuarioRepository.getUserByEmail(email).first()
                        }

                        // Filtrar evaluaciones según el rol del usuario
                        val filteredServerEvaluaciones = filterEvaluacionesByUserRole(serverEvaluaciones, currentUser)
                        
                        val localEvaluaciones = filteredServerEvaluaciones.map { EvaluacionPolinizacionMapper.fromResponse(it) }
                        Log.d(TAG, "Recibidas ${serverEvaluaciones.size} evaluaciones del servidor, filtradas a ${filteredServerEvaluaciones.size} según rol de usuario")
                        
                        dao.transaction {
                            val existingLocalPolinizaciones = dao.getEvaluaciones().first()
                            val serverIds = localEvaluaciones.map { it.serverId }.toSet()

                            // Delete local records not present on server (if no local changes)
                            existingLocalPolinizaciones.filter { it.serverId != null && !serverIds.contains(it.serverId) && it.isSynced }
                                .forEach { local ->
                                    dao.deleteEvaluacion(local)
                                    Log.d(TAG, "Deleted EvaluacionPolinizacion ID ${local.id} (not found on server)")
                                }

                            // Fetch all local EvaluacionGeneral records for mapping
                            val localGenerales = evaluacionGeneralDao.getAllEvaluacionesGenerales().first()
                            val serverIdToLocalIdMap = localGenerales.associate { it.serverId to it.id }

                            // Insert or update local database
                            localEvaluaciones.forEach { serverEval ->
                                // Map server's evaluacionGeneralId (serverId) to local EvaluacionGeneral id
                                val localGeneralId = serverIdToLocalIdMap[serverEval.evaluacionGeneralId]
                                if (localGeneralId == null) {
                                    Log.w(TAG, "No local EvaluacionGeneral found for serverId ${serverEval.evaluacionGeneralId}, skipping EvaluacionPolinizacion ${serverEval.serverId}")
                                    return@forEach // Skip if no matching EvaluacionGeneral exists
                                }

                                val existing = existingLocalPolinizaciones.find { it.serverId == serverEval.serverId }
                                val updatedEval = serverEval.copy(
                                    evaluacionGeneralId = localGeneralId // Use local ID
                                )
                                if (existing == null) {
                                    dao.insertEvaluacion(EvaluacionPolinizacionMapper.toDatabase(updatedEval.copy(isSynced = true)))
                                    Log.d(TAG, "Inserted EvaluacionPolinizacion serverId ${serverEval.serverId} with local evaluacionGeneralId $localGeneralId")
                                } else if (serverEval.timestamp > existing.timestamp) {
                                    dao.updateEvaluacion(
                                        EvaluacionPolinizacionMapper.toDatabase(updatedEval.copy(
                                            id = existing.id,
                                            isSynced = true
                                        ))
                                    )
                                    Log.d(TAG, "Updated EvaluacionPolinizacion ID ${existing.id} with server data")
                                }
                            }
                        }
                        notifyUser("Evaluaciones de polinización sincronizadas desde el servidor")
                    }
                } else {
                    Log.e(TAG, "Failed to fetch EvaluacionesPolinizacion: ${response.errorBody()?.string()}")
                    throw Exception("Failed to fetch pollination evaluations: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching EvaluacionesPolinizacion from server: ${e.message}", e)
                notifyUser("Error al sincronizar evaluaciones desde el servidor: ${e.message}")
            }
        }
    }

    // Filtrar evaluaciones de polinización según el rol del usuario
    private fun filterEvaluacionesByUserRole(
        serverEvaluaciones: List<EvaluacionResponse>,
        currentUser: Usuario?
    ): List<EvaluacionResponse> {
        return currentUser?.let { user ->
            when {
                // Administrador y Coordinador pueden ver todas las evaluaciones
                user.rol.equals(UserRoleConstants.ROLE_ADMIN, ignoreCase = true) ||
                        user.rol.equals(UserRoleConstants.ROLE_COORDINATOR, ignoreCase = true) ->
                    serverEvaluaciones
                
                // Evaluador solo puede ver evaluaciones de operarios de su finca
                user.rol.equals(UserRoleConstants.ROLE_EVALUATOR, ignoreCase = true) -> {
                    // Obtener IDs de operarios de la misma finca que el usuario
                    val operariosEnFinca = runBlocking {
                        operarioRepository.getAllOperarios()
                            .first()
                            .filter { it.fincaId == user.idFinca }
                            .map { it.id }
                    }
                    
                    Log.d(TAG, "Usuario evaluador con ID ${user.id}, finca ${user.idFinca}, filtrando para operarios: $operariosEnFinca")
                    
                    // Filtrar evaluaciones cuyo idPolinizador esté en la lista de operarios de la finca
                    serverEvaluaciones.filter { 
                        operariosEnFinca.contains(it.idpolinizador) || it.idevaluador == user.id
                    }.also {
                        Log.d(TAG, "Evaluaciones filtradas de ${serverEvaluaciones.size} a ${it.size} para evaluador")
                    }
                }
                else -> emptyList()
            }
        } ?: serverEvaluaciones
    }

    private suspend fun retryFailedEvaluaciones(
        failedEvaluaciones: List<EvaluacionPolinizacion>,
        serverGeneralId: Int,
        notificationManager: SyncNotificationManager
    ) {
        if (failedEvaluaciones.isEmpty()) return
        
        Log.d(TAG, "🔄 Reintentando ${failedEvaluaciones.size} evaluaciones fallidas")
        notificationManager.updateSyncMessage("Reintentando ${failedEvaluaciones.size} evaluaciones fallidas")
        
        val maxRetries = 5
        var currentRetry = 0
        var remainingEvaluaciones = failedEvaluaciones
        
        val syncedIds = mutableListOf<Int>()
        val stillFailedIds = mutableListOf<Int>()

        while (currentRetry < maxRetries && remainingEvaluaciones.isNotEmpty()) {
            Log.d(TAG, "🔄 Reintento ${currentRetry + 1}/${maxRetries} con ${remainingEvaluaciones.size} evaluaciones")
            notificationManager.updateSyncMessage("Reintento ${currentRetry + 1}/${maxRetries}: ${remainingEvaluaciones.size} eventos")
            
            // Pequeña espera exponencial entre reintentos
            if (currentRetry > 0) {
                val delayMs = 1000L * (1 shl currentRetry) // 1s, 2s, 4s, etc.
                Log.d(TAG, "⏱️ Esperando ${delayMs}ms antes del siguiente reintento")
                kotlinx.coroutines.delay(delayMs)
            }
            
            // Configurar seguimiento de progreso
            val totalToRetry = remainingEvaluaciones.size
            var currentItem = 0
            
            // Procesar cada evaluación individualmente
            remainingEvaluaciones.forEach { evaluacion ->
                try {
                    // Actualizar progreso
                    currentItem++
                    notificationManager.updateSyncProgress(
                        currentItem,
                        totalToRetry,
                        "Reintento ${currentRetry + 1}: evento ${currentItem}/${totalToRetry}"
                    )
                    
                    // Ajustamos el serverId solo en la solicitud al servidor
                    val evaluacionRequest = EvaluacionPolinizacionMapper.toRequest(evaluacion).copy(
                        evaluaciongeneralid = serverGeneralId
                    )
                    
                    val response = if (evaluacion.serverId != null) {
                        apiService.updateEvaluacion(evaluacion.serverId!!, evaluacionRequest)
                    } else {
                        apiService.createEvaluacion(evaluacionRequest)
                    }

                    if (response.isSuccessful && response.body() != null) {
                        val serverEval = response.body()!!
                        if (serverEval.id > 0) {
                            // Actualizamos la entidad local con el ID del servidor
                            val syncedEvaluacion = evaluacion.copy(
                                serverId = serverEval.id,
                                isSynced = true,
                                timestamp = serverEval.timestamp
                            )
                            dao.updateEvaluacion(EvaluacionPolinizacionMapper.toDatabase(syncedEvaluacion))
                            Log.d(TAG, "✅ Reintento exitoso para EvaluacionPolinizacion ID ${evaluacion.id}")
                            syncedIds.add(evaluacion.id!!)
                        } else {
                            Log.e(TAG, "❌ ID de servidor inválido en reintento para ID ${evaluacion.id}")
                            stillFailedIds.add(evaluacion.id!!)
                        }
                    } else {
                        Log.e(TAG, "❌ Error de servidor en reintento ${currentRetry + 1} para ID ${evaluacion.id}: ${response.code()}")
                        stillFailedIds.add(evaluacion.id!!)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Excepción en reintento ${currentRetry + 1} para ID ${evaluacion.id}: ${e.message}", e)
                    stillFailedIds.add(evaluacion.id!!)
                }
            }
            
            // Actualizar la lista de evaluaciones pendientes
            remainingEvaluaciones = remainingEvaluaciones.filter { it.id != null && !syncedIds.contains(it.id) }
            currentRetry++
            
            // Actualizar con progreso del reintento
            val retriedCount = totalToRetry - remainingEvaluaciones.size
            notificationManager.updateSyncMessage("Reintento ${currentRetry}/${maxRetries} completado: ${retriedCount}/${totalToRetry} recuperados")
        }
        
        // Notificar resultados al usuario
        if (remainingEvaluaciones.isEmpty()) {
            Log.d(TAG, "✅ Todas las ${failedEvaluaciones.size} evaluaciones se recuperaron en los reintentos")
            notificationManager.updateSyncMessage("✅ Se recuperaron todas las ${failedEvaluaciones.size} evaluaciones fallidas")
            notifyUser("✅ Se recuperaron todas las evaluaciones fallidas")
        } else {
            val message = "Recuperadas ${syncedIds.size}/${failedEvaluaciones.size}. ${remainingEvaluaciones.size} siguen fallando"
            Log.w(TAG, "⚠️ $message")
            notificationManager.updateSyncMessage("⚠️ $message")
            notifyUser("⚠️ $message")
        }
    }
}