package com.agrojurado.sfmappv2.data.repository

import android.content.Context
import android.util.Log
import com.agrojurado.sfmappv2.data.local.dao.AreaDao
import com.agrojurado.sfmappv2.data.mapper.AreaMapper
import com.agrojurado.sfmappv2.data.remote.api.AreaApiService
import com.agrojurado.sfmappv2.domain.model.Area
import com.agrojurado.sfmappv2.domain.repository.AreaRepository
import com.agrojurado.sfmappv2.data.remote.dto.common.utils.Utils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AreaRepositoryImpl @Inject constructor(
    private val areaDao: AreaDao,
    private val areaApiService: AreaApiService,
    @ApplicationContext private val context: Context
) : AreaRepository {

    companion object {
        private const val TAG = "AreaRepository"
    }

    private fun isNetworkAvailable(): Boolean = Utils.isNetworkAvailable(context)

    private fun showSyncAlert(message: String) {
        Utils.showAlert(context, message)
    }

    private fun logServerError(response: retrofit2.Response<*>, logMessage: String) {
        Utils.logError(TAG, Exception("Server error (${response.code()}): ${response.errorBody()?.string()}"), logMessage)
    }

    override fun getAllAreas(): Flow<List<Area>> {
        return areaDao.getAllAreas().map { entities ->
            entities.map(AreaMapper::toDomain)
        }
    }

    override suspend fun getAreaById(id: Int): Area? {
        return areaDao.getAreaById(id)?.let(AreaMapper::toDomain)
    }

    override suspend fun insertArea(area: Area): Long {
        try {
            if (isNetworkAvailable()) {
                val areaRequest = AreaMapper.toRequest(area)
                val response = areaApiService.createArea(areaRequest)
                syncAreas()

                if (response.isSuccessful && response.body() != null) {
                    val serverArea = AreaMapper.fromResponse(response.body()!!)
                    val areaWithServerId = area.copy(id = serverArea.id, isSynced = true)
                    return areaDao.insertArea(AreaMapper.toDatabase(areaWithServerId))
                } else {
                    logServerError(response, "Error creando el área en el servidor")
                    throw Exception("Error del servidor al crear el área")
                }
            } else {
                val localArea = area.copy(id = 0, isSynced = false)
                val localId = areaDao.insertArea(AreaMapper.toDatabase(localArea))
                showSyncAlert("Guardado localmente, se sincronizará cuando haya conexión")
                return localId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creando área: ${e.message}")
            throw Exception("Error al guardar el área: ${e.message}")
        }
    }

    override suspend fun updateArea(area: Area) {
        try {
            areaDao.updateArea(AreaMapper.toDatabase(area).apply { isSynced = false })

            if (isNetworkAvailable() && area.id != null) {
                val response = areaApiService.updateArea(AreaMapper.toRequest(area))
                if (response.isSuccessful) {
                    areaDao.updateArea(AreaMapper.toDatabase(area).apply { isSynced = true })
                } else {
                    logServerError(response, "Error actualizando el área en el servidor")
                    throw Exception("Error del servidor al actualizar el área")
                }
            } else {
                showSyncAlert("Sin conexión, área actualizada localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando área: ${e.message}")
            showSyncAlert("Error al actualizar: ${e.message}")
            throw e
        }
    }

    override suspend fun deleteArea(area: Area) {
        try {
            areaDao.deleteArea(AreaMapper.toDatabase(area))

            if (isNetworkAvailable() && area.id != null) {
                val response = areaApiService.deleteArea(area.id)
                if (response.isSuccessful) {
                    Log.d(TAG, "Área eliminada correctamente en el servidor")
                } else {
                    logServerError(response, "Error al eliminar área en el servidor")
                    throw Exception("Error del servidor al eliminar el área")
                }
            } else {
                showSyncAlert("Sin conexión, área eliminada localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando área: ${e.message}")
            showSyncAlert("Error al eliminar: ${e.message}")
            throw e
        }
    }

    override suspend fun deleteAllAreas() {
        val errorList = mutableListOf<String>()
        var hasLocalChanges = false

        try {
            areaDao.deleteAllAreas()
            hasLocalChanges = true
            Log.d(TAG, "Datos locales eliminados")

            if (isNetworkAvailable()) {
                val serverResponse = areaApiService.getAreas()
                if (!serverResponse.isSuccessful) {
                    throw Exception("Error al obtener áreas del servidor")
                }

                val serverAreas = serverResponse.body()?.filterNotNull() ?: emptyList()
                serverAreas.forEach { serverArea ->
                    try {
                        val response = areaApiService.deleteArea(serverArea.id)
                        if (!response.isSuccessful) {
                            val error = "Error al eliminar área ${serverArea.id}: ${response.errorBody()?.string()}"
                            Log.e(TAG, error)
                            errorList.add(error)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al eliminar área en el servidor: ${e.message}")
                        errorList.add("Error al eliminar área ${serverArea.id} en el servidor: ${e.message}")
                    }
                }

                if (errorList.isNotEmpty()) {
                    throw Exception("Errores al eliminar áreas en el servidor:\n${errorList.joinToString("\n")}")
                }
                showSyncAlert("Todas las áreas eliminadas correctamente del servidor")
            } else {
                showSyncAlert("Sin conexión, áreas eliminadas localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar todas las áreas: ${e.message}")
            val message = if (hasLocalChanges) {
                "Áreas eliminadas localmente, pero hubo errores en el servidor: ${e.message}"
            } else {
                "Error al eliminar áreas: ${e.message}"
            }
            showSyncAlert(message)
            throw Exception(message)
        }
    }

    override suspend fun syncAreas() {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "No hay conexión disponible para la sincronización")
            showSyncAlert("Sin conexión, usando datos locales")
            return
        }

        try {
            val unsyncedAreas = areaDao.getAllAreas().first().filter { !it.isSynced }

            if (unsyncedAreas.isNotEmpty()) {
                Log.d(TAG, "Áreas no sincronizadas encontradas: ${unsyncedAreas.size}")
                unsyncedAreas.forEach { localArea ->
                    try {
                        val areaRequest = AreaMapper.toRequest(AreaMapper.toDomain(localArea))

                        if (areaRequest.id == null || areaRequest.descripcion.isNullOrBlank()) {
                            Log.e(TAG, "Campos obligatorios faltantes para el área: ${localArea.id}")
                            return@forEach
                        }

                        val response = areaApiService.createArea(areaRequest)
                        if (response.isSuccessful && response.body() != null) {
                            val serverArea = AreaMapper.fromResponse(response.body()!!)
                            val updatedArea = localArea.copy(id = serverArea.id, isSynced = true)
                            areaDao.updateArea(updatedArea)
                            Log.d(TAG, "Área sincronizada correctamente con el ID: ${updatedArea.id}")
                        } else {
                            Log.e(TAG, "Error al sincronizar área ${localArea.id}: ${response.errorBody()?.string()}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al sincronizar área ${localArea.id}: ${e.message}")
                    }
                }
            }

            val response = areaApiService.getAreas()
            if (!response.isSuccessful) {
                logServerError(response, "Error obteniendo áreas del servidor")
                throw Exception("Error al obtener áreas del servidor")
            }

            val serverAreas = response.body()?.filterNotNull() ?: emptyList()
            if (serverAreas.isEmpty()) {
                Log.d(TAG, "El servidor no tiene áreas, no se realiza la sincronización.")
                showSyncAlert("El servidor no tiene áreas para sincronizar.")
                return
            }

            val localAreasMap = areaDao.getAllAreas().first().associateBy { it.id }

            localAreasMap.values.filter { it.id != null && !serverAreas.any { serverArea -> serverArea.id == it.id } }
                .forEach { localArea ->
                    areaDao.deleteArea(localArea)
                    Log.d(TAG, "Área eliminada localmente porque no existe en el servidor: ${localArea.id}")
                }

            serverAreas.forEach { serverArea ->
                val domainArea = AreaMapper.fromResponse(serverArea)
                val localArea = localAreasMap[serverArea.id]
                if (localArea == null) {
                    areaDao.insertArea(AreaMapper.toDatabase(domainArea).apply { isSynced = true })
                } else {
                    areaDao.updateArea(AreaMapper.toDatabase(domainArea).apply { isSynced = true })
                }
            }

            Log.d(TAG, "Sincronización completa")
            //showSyncAlert("Sincronización completada exitosamente.")
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
            // Primero sincronizar áreas porque son requeridas por operarios
            val response = areaApiService.getAreas()
            if (!response.isSuccessful) {
                logServerError(response, "Error en la sincronización completa")
                return false
            }

            val serverAreas = response.body()?.filterNotNull() ?: emptyList()

            // Usar transacción para mantener la integridad
            areaDao.transaction {
                // NO eliminar todas las áreas de golpe para evitar problemas de FK
                val localAreasMap = areaDao.getAllAreas().first().associateBy { it.id }

                // Primero insertar/actualizar áreas nuevas
                serverAreas.forEach { serverArea ->
                    val domainArea = AreaMapper.fromResponse(serverArea)
                    val localArea = localAreasMap[serverArea.id]

                    if (localArea == null) {
                        areaDao.insertArea(AreaMapper.toDatabase(domainArea).apply { isSynced = true })
                        Log.d(TAG, "Área insertada desde el servidor: ${domainArea.id}")
                    } else {
                        areaDao.updateArea(AreaMapper.toDatabase(domainArea).apply { isSynced = true })
                        Log.d(TAG, "Área actualizada desde el servidor: ${domainArea.id}")
                    }
                }

                // Luego eliminar áreas que ya no existen en el servidor
                // pero solo si no tienen operarios referenciándolas
                localAreasMap.values
                    .filter { localArea ->
                        !serverAreas.any { it.id == localArea.id }
                    }
                    .forEach { areaToDelete ->
                        try {
                            areaDao.deleteArea(areaToDelete)
                            Log.d(TAG, "Área eliminada: ${areaToDelete.id}")
                        } catch (e: Exception) {
                            Log.w(TAG, "No se pudo eliminar el área ${areaToDelete.id} porque tiene operarios asociados")
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
