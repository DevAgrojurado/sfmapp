package com.agrojurado.sfmappv2.data.repository

import android.content.Context
import android.util.Log
import com.agrojurado.sfmappv2.data.local.dao.EvaluacionPolinizacionDao
import com.agrojurado.sfmappv2.data.mapper.EvaluacionPolinizacionMapper
import com.agrojurado.sfmappv2.data.mapper.LoteMapper
import com.agrojurado.sfmappv2.data.remote.api.EvaluacionApiService
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import com.agrojurado.sfmappv2.domain.repository.EvaluacionPolinizacionRepository
import com.agrojurado.sfmappv2.data.remote.dto.common.utils.Utils
import com.agrojurado.sfmappv2.data.repository.LoteRepositoryImpl.Companion
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class EvaluacionPolinizacionRepositoryImpl @Inject constructor(
    private val dao: EvaluacionPolinizacionDao,
    private val apiService: EvaluacionApiService,
    @ApplicationContext private val context: Context
) : EvaluacionPolinizacionRepository {

    companion object {
        private const val TAG = "EvaluacionRepository"
    }

    private fun isNetworkAvailable(): Boolean = Utils.isNetworkAvailable(context)

    private fun showSyncAlert(message: String) {
        Utils.showAlert(context, message)
    }

    private fun logServerError(response: retrofit2.Response<*>, logMessage: String) {
        Utils.logError(TAG, Exception("Server error (${response.code()}): ${response.errorBody()?.string()}"), logMessage)
    }

    override fun getEvaluaciones(): Flow<List<EvaluacionPolinizacion>> {
        return dao.getEvaluaciones().map { entities ->
            entities.map(EvaluacionPolinizacionMapper::toDomain)
        }
    }

    override suspend fun getEvaluacionById(id: Int): EvaluacionPolinizacion? {
        return dao.getEvaluacionById(id)?.let(EvaluacionPolinizacionMapper::toDomain)
    }

    override suspend fun getLastEvaluacion(): EvaluacionPolinizacion? {
        return dao.getLastEvaluacion()?.let(EvaluacionPolinizacionMapper::toDomain)
    }

    override suspend fun checkPalmExists(semana: Int, lote: Int, palma: Int, idPolinizador: Int): Boolean {
        return dao.checkPalmExists(semana, lote, palma, idPolinizador) > 0
    }

    override suspend fun insertEvaluacion(evaluacion: EvaluacionPolinizacion): Long {
        try {
            if (isNetworkAvailable()) {
                // Verificar si ya existe en el servidor (por id)
                val evaluacionRequest = EvaluacionPolinizacionMapper.toRequest(evaluacion)
                val response = apiService.createEvaluacion(evaluacionRequest)

                if (response.isSuccessful && response.body() != null) {
                    val serverEvaluacion = EvaluacionPolinizacionMapper.fromResponse(response.body()!!)
                    val evaluacionWithServerId = evaluacion.copy(id = serverEvaluacion.id, isSynced = true)

                    // Insertar en la base de datos local
                    return dao.insertEvaluacion(EvaluacionPolinizacionMapper.toDatabase(evaluacionWithServerId))
                } else {
                    logServerError(response, "Error creando la evaluación en el servidor")
                    throw Exception("Error del servidor al crear la evaluación")
                }
            } else {
                // Guardar localmente si no hay conexión
                val localEvaluacion = evaluacion.copy(id = 0, isSynced = false)
                val localId = dao.insertEvaluacion(EvaluacionPolinizacionMapper.toDatabase(localEvaluacion))
                showSyncAlert("Guardado localmente, se sincronizará cuando haya conexión")
                return localId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creando evaluación: ${e.message}")
            throw Exception("Error al guardar la evaluación: ${e.message}")
        }
    }

    override suspend fun updateEvaluacion(evaluacion: EvaluacionPolinizacion) {
        try {
            // Actualizar localmente
            dao.updateEvaluacion(EvaluacionPolinizacionMapper.toDatabase(evaluacion).apply { isSynced = false })

            if (isNetworkAvailable() && evaluacion.id != null) {
                // Si hay conexión, sincronizar con el servidor
                val response = apiService.updateEvaluacion(EvaluacionPolinizacionMapper.toRequest(evaluacion))
                if (response.isSuccessful) {
                    // Marcar como sincronizada
                    dao.updateEvaluacion(EvaluacionPolinizacionMapper.toDatabase(evaluacion).apply { isSynced = true })
                } else {
                    logServerError(response, "Error actualizando la evaluación en el servidor")
                    throw Exception("Error del servidor al actualizar la evaluación")
                }
            } else {
                // Sin conexión, solo actualizar localmente
                showSyncAlert("Sin conexión, evaluación actualizada localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando evaluación: ${e.message}")
            showSyncAlert("Error al actualizar: ${e.message}")
            throw e
        }
    }


    override suspend fun deleteEvaluacion(evaluacion: EvaluacionPolinizacion) {
        try {
            // Eliminar localmente
            dao.deleteEvaluacion(EvaluacionPolinizacionMapper.toDatabase(evaluacion))

            if (isNetworkAvailable() && evaluacion.id != null) {
                // Si hay conexión, eliminar también en el servidor
                val response = apiService.deleteEvaluacion(evaluacion.id)
                if (response.isSuccessful) {
                    Log.d(TAG, "Evaluación eliminada correctamente en el servidor")
                } else {
                    logServerError(response, "Error al eliminar evaluación en el servidor")
                    throw Exception("Error del servidor al eliminar la evaluación")
                }
            } else {
                // Sin conexión, solo eliminamos localmente
                showSyncAlert("Sin conexión, evaluación eliminada localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando evaluación: ${e.message}")
            showSyncAlert("Error al eliminar: ${e.message}")
            throw e
        }
    }

    override suspend fun deleteAllEvaluaciones() {
        val errorList = mutableListOf<String>()
        var hasLocalChanges = false

        try {
            // Eliminar todas las evaluaciones locales
            dao.deleteAllEvaluaciones()
            hasLocalChanges = true
            Log.d(TAG, "Evaluaciones locales eliminadas.")

            if (isNetworkAvailable()) {
                // Eliminar del servidor las evaluaciones que están localmente
                val response = apiService.getEvaluaciones()
                if (!response.isSuccessful) {
                    throw Exception("Error al obtener evaluaciones del servidor")
                }

                val serverEvaluaciones = response.body()?.filterNotNull() ?: emptyList()

                serverEvaluaciones.forEach { serverEvaluacion ->
                    try {
                        val response = apiService.deleteEvaluacion(serverEvaluacion.id)
                        if (!response.isSuccessful) {
                            val error = "Error al eliminar evaluación ${serverEvaluacion.id}: ${response.errorBody()?.string()}"
                            Log.e(TAG, error)
                            errorList.add(error)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al eliminar evaluación ${serverEvaluacion.id}: ${e.message}")
                        errorList.add("Error al eliminar evaluación ${serverEvaluacion.id}: ${e.message}")
                    }
                }

                if (errorList.isNotEmpty()) {
                    throw Exception("Errores al eliminar evaluación en el servidor:\n${errorList.joinToString("\n")}")
                }

                showSyncAlert("Todas las evaluaciones eliminadas correctamente del servidor")
            } else {
                // Sin conexión, solo eliminamos localmente
                showSyncAlert("Sin conexión, evaluaciones eliminadas localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar todas las evaluaciones: ${e.message}")
            val message = if (hasLocalChanges) {
                "Evaluaciones eliminadas localmente, pero hubo errores en el servidor: ${e.message}"
            } else {
                "Error al eliminar evaluaciones: ${e.message}"
            }
            showSyncAlert(message)
            throw Exception(message)
        }
    }

    override suspend fun syncEvaluaciones() {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "No hay conexión disponible para la sincronización")
            showSyncAlert("Sin conexión, usando datos locales")
            return
        }

        try {
            // Obtener primero las evaluaciones del servidor para tener una referencia
            val serverResponse = apiService.getEvaluaciones()
            if (!serverResponse.isSuccessful) {
                throw Exception("Error al obtener evaluaciones del servidor")
            }
            val serverEvaluaciones = serverResponse.body()?.filterNotNull()?.associateBy { it.id } ?: emptyMap()

            // Sincronizar evaluaciones locales no sincronizadas
            val unsyncedEvaluaciones = dao.getEvaluaciones().first().filter { !it.isSynced }

            if (unsyncedEvaluaciones.isNotEmpty()) {
                Log.d(TAG, "Evaluaciones no sincronizadas encontradas: ${unsyncedEvaluaciones.size}")
                unsyncedEvaluaciones.forEach { localEvaluacion ->
                    try {
                        // Verificar si esta evaluación ya existe en el servidor
                        if (localEvaluacion.id != null && serverEvaluaciones.containsKey(localEvaluacion.id)) {
                            // Si ya existe, solo actualizamos el estado de sincronización
                            dao.updateEvaluacion(localEvaluacion.copy(isSynced = true))
                            Log.d(TAG, "Evaluación ${localEvaluacion.id} ya existe en el servidor, marcada como sincronizada")
                            return@forEach
                        }

                        val evaluacionRequest = EvaluacionPolinizacionMapper.toRequest(
                            EvaluacionPolinizacionMapper.toDomain(localEvaluacion)
                        )

                        if (evaluacionRequest.id == null) {
                            Log.e(TAG, "Campos obligatorios faltantes para la evaluación: ${localEvaluacion.id}")
                            return@forEach
                        }

                        val response = apiService.createEvaluacion(evaluacionRequest)
                        if (response.isSuccessful && response.body() != null) {
                            val serverEvaluacion = EvaluacionPolinizacionMapper.fromResponse(response.body()!!)
                            val updatedEvaluacion = localEvaluacion.copy(
                                id = serverEvaluacion.id,
                                isSynced = true
                            )
                            dao.updateEvaluacion(updatedEvaluacion)
                            Log.d(TAG, "Evaluación sincronizada correctamente con el ID: ${updatedEvaluacion.id}")
                        } else {
                            Log.e(TAG, "Error al sincronizar evaluación ${localEvaluacion.id}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al sincronizar evaluación ${localEvaluacion.id}: ${e.message}")
                    }
                }
            }

            // Actualizar la base de datos local con las evaluaciones del servidor
            val localEvaluacionesMap = dao.getEvaluaciones().first().associateBy { it.id }

            // Actualizar o insertar las evaluaciones del servidor
            serverEvaluaciones.values.forEach { serverEvaluacion ->
                val domainEvaluacion = EvaluacionPolinizacionMapper.fromResponse(serverEvaluacion)
                val localEvaluacion = localEvaluacionesMap[serverEvaluacion.id]

                if (localEvaluacion == null) {
                    dao.insertEvaluacion(EvaluacionPolinizacionMapper.toDatabase(domainEvaluacion).apply {
                        isSynced = true
                    })
                } else if (!localEvaluacion.isSynced) {
                    dao.updateEvaluacion(EvaluacionPolinizacionMapper.toDatabase(domainEvaluacion).apply {
                        isSynced = true
                    })
                }
            }

            Log.d(TAG, "Sincronización completada exitosamente")
            showSyncAlert("Sincronización completada exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la sincronización: ${e.message}")
            showSyncAlert("Error durante la sincronización: ${e.message}")
        }
    }

    override suspend fun fullSync(): Boolean {
        if (!isNetworkAvailable()) {
            showSyncAlert("Sin conexión a Internet")
            return false
        }

        try {
            val response = apiService.getEvaluaciones()
            if (!response.isSuccessful) {
                logServerError(response, "Error en la sincronización completa")
                return false
            }

            val serverEvaluaciones = response.body()?.filterNotNull() ?: emptyList()

            dao.transaction {
                val localEvalucionesMap = dao.getEvaluaciones().first().associateBy { it.id }

                serverEvaluaciones.forEach { serverEvaluacion ->
                    val domainEvaluacion = EvaluacionPolinizacionMapper.fromResponse(serverEvaluacion)
                    val localEvaluacion = localEvalucionesMap[serverEvaluacion.id]

                    if (localEvaluacion == null) {
                        dao.insertEvaluacion(EvaluacionPolinizacionMapper.toDatabase(domainEvaluacion).apply { isSynced = true })
                        Log.d(TAG, "Evaluacion insertado desde el servidor: ${domainEvaluacion.id}")
                    } else {
                        dao.updateEvaluacion(EvaluacionPolinizacionMapper.toDatabase(domainEvaluacion).apply { isSynced = true })
                        Log.d(TAG, "Lote actualizado desde el servidor: ${domainEvaluacion.id}")
                    }
                }

                localEvalucionesMap.values
                    .filter { localEvaluacion ->
                        !serverEvaluaciones.any { it.id == localEvaluacion.id }
                    }
                    .forEach { evaluacionToDelete ->
                        try {
                            dao.deleteEvaluacion(evaluacionToDelete)
                            Log.d(TAG, "Lote eliminado: ${evaluacionToDelete.id}")
                        } catch (e: Exception) {
                            Log.w(TAG, "No se pudo eliminar la evaluacion ${evaluacionToDelete.id} porque tiene referencias asociadas")
                        }
                    }
            }

            Log.d(TAG, "Sincronización completa exitosa.")
            showSyncAlert("Sincronización completa")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error en la sincronización completa: ${e.message}", e)
            showSyncAlert("Error durante la sincronización completa: ${e.message}")
            return false
        }
    }
}