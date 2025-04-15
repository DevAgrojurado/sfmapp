package com.agrojurado.sfmappv2.data.repository

import android.content.Context
import android.util.Log
import com.agrojurado.sfmappv2.data.local.dao.EvaluacionGeneralDao
import com.agrojurado.sfmappv2.data.local.dao.EvaluacionPolinizacionDao
import com.agrojurado.sfmappv2.data.mapper.EvaluacionPolinizacionMapper
import com.agrojurado.sfmappv2.data.remote.api.EvaluacionApiService
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import com.agrojurado.sfmappv2.domain.repository.EvaluacionPolinizacionRepository
import com.agrojurado.sfmappv2.data.remote.dto.common.utils.Utils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class EvaluacionPolinizacionRepositoryImpl @Inject constructor(
    private val dao: EvaluacionPolinizacionDao,
    private val apiService: EvaluacionApiService,
    private val evaluacionGeneralDao: EvaluacionGeneralDao,
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
            return
        }

        withContext(Dispatchers.IO) {
            evaluaciones.forEach { localEvaluacion ->
                try {
                    // No modificamos evaluacionGeneralId localmente, mantenemos el ID local original
                    val updatedLocalEvaluacion = localEvaluacion.copy(
                        isSynced = false // Reset para asegurar sincronización
                    )

                    // Actualizamos localmente sin cambiar evaluacionGeneralId
                    dao.updateEvaluacion(EvaluacionPolinizacionMapper.toDatabase(updatedLocalEvaluacion))
                    Log.d(TAG, "Updated local EvaluacionPolinizacion ID ${localEvaluacion.id} with original evaluacionGeneralId ${localEvaluacion.evaluacionGeneralId}")

                    // Ajustamos el serverId solo en la solicitud al servidor
                    val evaluacionRequest = EvaluacionPolinizacionMapper.toRequest(updatedLocalEvaluacion).copy(
                        evaluaciongeneralid = serverGeneralId // Usamos el serverId para el servidor
                    )
                    val response = if (localEvaluacion.serverId != null) {
                        apiService.updateEvaluacion(localEvaluacion.serverId!!, evaluacionRequest)
                    } else {
                        apiService.createEvaluacion(evaluacionRequest)
                    }

                    if (response.isSuccessful && response.body() != null) {
                        val serverEvaluacion = response.body()!!
                        if (serverEvaluacion.id <= 0) {
                            Log.e(TAG, "Invalid server ID (${serverEvaluacion.id})")
                            return@forEach
                        }

                        val syncedEvaluacion = updatedLocalEvaluacion.copy(
                            serverId = serverEvaluacion.id,
                            isSynced = true,
                            timestamp = serverEvaluacion.timestamp
                        )
                        dao.updateEvaluacion(EvaluacionPolinizacionMapper.toDatabase(syncedEvaluacion))
                        Log.d(TAG, "Synced EvaluacionPolinizacion ID ${localEvaluacion.id} with serverId ${serverEvaluacion.id}")
                    } else {
                        Log.e(TAG, "Sync failed for EvaluacionPolinizacion ${localEvaluacion.id}: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception syncing EvaluacionPolinizacion ${localEvaluacion.id}: ${e.message}", e)
                }
            }
            notifyUser("${evaluaciones.size} evaluaciones sincronizadas")
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
                        val localEvaluaciones = serverEvaluaciones.map { EvaluacionPolinizacionMapper.fromResponse(it) }
                        dao.transaction {
                            val existingLocalPolinizaciones = dao.getEvaluaciones().first()
                            val serverIds = localEvaluaciones.map { it.serverId }.toSet()

                            // Delete local records not present on server (if no local changes)
                            existingLocalPolinizaciones.filter { it.serverId != null && !serverIds.contains(it.serverId) }
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
}