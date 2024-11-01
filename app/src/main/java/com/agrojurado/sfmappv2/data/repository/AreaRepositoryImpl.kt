package com.agrojurado.sfmappv2.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import com.agrojurado.sfmappv2.data.local.dao.AreaDao
import com.agrojurado.sfmappv2.data.mapper.AreaMapper
import com.agrojurado.sfmappv2.data.remote.api.AreaApiService
import com.agrojurado.sfmappv2.domain.model.Area
import com.agrojurado.sfmappv2.domain.repository.AreaRepository
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

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                )
    }

    private fun showSyncAlert(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
        var localId = 0L
        try {
            // Guardar el área localmente y obtener el ID local
            localId = areaDao.insertArea(AreaMapper.toDatabase(area))

            // Intentar sincronizar si hay conexión de red
            if (isNetworkAvailable()) {
                val areaRequest = AreaMapper.toRequest(area.copy(id = localId.toInt()))
                val response = areaApiService.createArea(areaRequest)

                if (response.isSuccessful && response.body() != null) {
                    // Actualizar con los datos del servidor si la sincronización fue exitosa
                    val serverArea = AreaMapper.fromResponse(response.body()!!)
                    areaDao.updateArea(AreaMapper.toDatabase(serverArea))
                    return serverArea.id?.toLong() ?: localId
                } else {
                    logServerError(response, "Error creando el área en el servidor")
                    throw Exception("Error del servidor al crear el área")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating area: ${e.message}")

            // Si no se guardó localmente, lanzar excepción
            if (localId == 0L) {
                throw Exception("Error al guardar el área: ${e.message}")
            }
        }

        // Mostrar alerta solo si no hay conexión y se guardó localmente
        if (!isNetworkAvailable() && localId != 0L) {
            showSyncAlert("Área guardada localmente, se sincronizará cuando haya conexión")
        }
        return localId
    }


    override suspend fun updateArea(area: Area) {
        try {
            areaDao.updateArea(AreaMapper.toDatabase(area))
            if (isNetworkAvailable() && area.id != null) {
                val response = areaApiService.updateArea(AreaMapper.toRequest(area))

                if (!response.isSuccessful) {
                    logServerError(response, "Error updating area on server")
                    throw Exception("Error del servidor al actualizar el área")
                }
            } else {
                showSyncAlert("Sin conexión, área actualizada localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating area: ${e.message}")
            showSyncAlert("Error al actualizar: ${e.message}")
            throw e
        }
    }

    override suspend fun deleteArea(area: Area) {
        try {
            areaDao.deleteArea(AreaMapper.toDatabase(area))
            if (isNetworkAvailable() && area.id != null) {
                val response = areaApiService.deleteArea(area.id)
                if (!response.isSuccessful) {
                    logServerError(response, "Error deleting area on server")
                    throw Exception("Error del servidor al eliminar el área")
                }
            } else {
                showSyncAlert("Sin conexión, área eliminada localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting area: ${e.message}")
            showSyncAlert("Error al eliminar: ${e.message}")
            throw e
        }
    }

    override suspend fun deleteAllAreas() {
        val errorList = mutableListOf<String>()
        var hasLocalChanges = false

        try {
            val localAreas = areaDao.getAllAreas().first()
            areaDao.deleteAllAreas()
            hasLocalChanges = true

            if (isNetworkAvailable()) {
                val serverResponse = areaApiService.getAreas()
                if (!serverResponse.isSuccessful) {
                    throw Exception("Error al obtener áreas del servidor")
                }

                val serverAreas = serverResponse.body()?.filterNotNull() ?: emptyList()
                val allAreas = (serverAreas.map { it.id } + localAreas.mapNotNull { it.id }).distinct()

                allAreas.forEach { areaId ->
                    areaId?.let {
                        val response = areaApiService.deleteArea(it)
                        if (!response.isSuccessful) {
                            val error = "Error al eliminar área $it: ${response.errorBody()?.string()}"
                            Log.e(TAG, error)
                            errorList.add(error)
                        }
                    }
                }

                if (errorList.isNotEmpty()) {
                    throw Exception("Errores al eliminar áreas en el servidor:\n${errorList.joinToString("\n")}")
                }

                showSyncAlert("Todas las áreas eliminadas correctamente")
            } else {
                showSyncAlert("Sin conexión, áreas eliminadas localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in deleteAllAreas: ${e.message}")
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
            Log.d(TAG, "No connection available for sync")
            showSyncAlert("Sin conexión, usando datos locales")
            return
        }

        try {
            // Obtener todas las áreas desde el servidor
            val response = areaApiService.getAreas()
            if (!response.isSuccessful) {
                throw Exception("Error al obtener áreas del servidor")
            }

            val serverAreas = response.body()?.filterNotNull() ?: emptyList()
            val localAreas = areaDao.getAllAreas().first()
            val serverAreasMap = serverAreas.associateBy { it.id }
            val localAreasMap = localAreas.associateBy { it.id }

            // Sincronizar áreas desde el servidor
            serverAreas.forEach { serverArea ->
                val localArea = localAreasMap[serverArea.id]
                val domainArea = AreaMapper.fromResponse(serverArea)

                if (localArea == null) {
                    areaDao.insertArea(AreaMapper.toDatabase(domainArea))
                } else {
                    areaDao.updateArea(AreaMapper.toDatabase(domainArea))
                }
            }

            // Sincronizar áreas locales que no están en el servidor
            localAreas.filter { it.id == null || !serverAreasMap.containsKey(it.id) }
                .forEach { localArea ->
                    try {
                        val domainArea = AreaMapper.toDomain(localArea)
                        val createResponse = areaApiService.createArea(AreaMapper.toRequest(domainArea))

                        if (createResponse.isSuccessful && createResponse.body() != null) {
                            val newServerArea = AreaMapper.fromResponse(createResponse.body()!!)
                            areaDao.updateArea(AreaMapper.toDatabase(newServerArea))
                        } else {
                            logServerError(createResponse, "Error syncing local area")
                            throw Exception("Error al sincronizar área local con el servidor")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing local area: ${e.message}")
                        // Manejar el error adecuadamente
                    }
                }

            showSyncAlert("Sincronización completada")
        } catch (e: Exception) {
            Log.e(TAG, "Sync error: ${e.message}")
            throw Exception("Error en la sincronización: ${e.message}")
        }
    }


    override suspend fun fullSync(): Boolean {
        if (!isNetworkAvailable()) {
            showSyncAlert("Sin conexión a Internet")
            return false
        }

        try {
            val response = areaApiService.getAreas()
            if (!response.isSuccessful) {
                throw Exception("Error al obtener datos del servidor")
            }

            val serverAreas = response.body()?.filterNotNull() ?: emptyList()
            val localAreas = areaDao.getAllAreas().first()

            localAreas.filter { it.id == null }.forEach { localArea ->
                val createResponse = areaApiService.createArea(
                    AreaMapper.toRequest(AreaMapper.toDomain(localArea))
                )
                if (!createResponse.isSuccessful) {
                    throw Exception("Error al sincronizar área local")
                }
            }

            areaDao.deleteAllAreas()
            serverAreas.forEach { serverArea ->
                areaDao.insertArea(AreaMapper.toDatabase(AreaMapper.fromResponse(serverArea)))
            }

            showSyncAlert("Sincronización completada")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Full sync error: ${e.message}")
            throw Exception("Error en la sincronización completa: ${e.message}")
        }
    }

    private fun logServerError(response: retrofit2.Response<*>, logMessage: String) {
        val error = "Server error (${response.code()}): ${response.errorBody()?.string()}"
        Log.e(TAG, "$logMessage - $error")
    }
}
