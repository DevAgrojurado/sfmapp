package com.agrojurado.sfmappv2.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import com.agrojurado.sfmappv2.data.local.dao.CargoDao
import com.agrojurado.sfmappv2.data.mapper.CargoMapper
import com.agrojurado.sfmappv2.data.remote.api.CargoApiService
import com.agrojurado.sfmappv2.domain.model.Cargo
import com.agrojurado.sfmappv2.domain.repository.CargoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CargoRepositoryImpl @Inject constructor(
    private val cargoDao: CargoDao,
    private val cargoApiService: CargoApiService,
    @ApplicationContext private val context: Context
) : CargoRepository {

    companion object {
        private const val TAG = "CargoRepository"
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

    override fun getAllCargos(): Flow<List<Cargo>> {
        return cargoDao.getAllCargos().map { entities ->
            entities.map(CargoMapper::toDomain)
        }
    }

    override suspend fun getCargoById(id: Int): Cargo? {
        return cargoDao.getCargoById(id)?.let(CargoMapper::toDomain)
    }

    override suspend fun insertCargo(cargo: Cargo): Long {
        var localId = 0L
        try {
            localId = cargoDao.insertCargo(CargoMapper.toDatabase(cargo))
            if (isNetworkAvailable()) {
                val cargoRequest = CargoMapper.toRequest(cargo.copy(id = localId.toInt()))
                val response = cargoApiService.createCargo(cargoRequest)

                if (response.isSuccessful && response.body() != null) {
                    val serverCargo = CargoMapper.fromResponse(response.body()!!)
                    cargoDao.updateCargo(CargoMapper.toDatabase(serverCargo))
                    return serverCargo.id?.toLong() ?: localId
                } else {
                    logServerError(response, "Error creating cargo on server")
                    throw Exception("Error del servidor al crear el cargo")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating cargo: ${e.message}")
            if (localId != 0L) {
                throw Exception("Error al guardar el cargo: ${e.message}")
            }
        }
        if (!isNetworkAvailable() && localId != 0L) {
            showSyncAlert("Cargo guardado localmente, se sincronizará cuando haya conexión")
        }
        return localId
    }

    override suspend fun updateCargo(cargo: Cargo) {
        try {
            cargoDao.updateCargo(CargoMapper.toDatabase(cargo))
            if (isNetworkAvailable() && cargo.id != null) {
                val response = cargoApiService.updateCargo(CargoMapper.toRequest(cargo))

                if (!response.isSuccessful) {
                    logServerError(response, "Error updating cargo on server")
                    throw Exception("Error del servidor al actualizar el cargo")
                }
            } else {
                showSyncAlert("Sin conexión, cargo actualizado localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating cargo: ${e.message}")
            showSyncAlert("Error al actualizar: ${e.message}")
            throw e
        }
    }

    override suspend fun deleteCargo(cargo: Cargo) {
        try {
            cargoDao.deleteCargo(CargoMapper.toDatabase(cargo))
            if (isNetworkAvailable() && cargo.id != null) {
                val response = cargoApiService.deleteCargo(cargo.id)
                if (!response.isSuccessful) {
                    logServerError(response, "Error deleting cargo on server")
                    throw Exception("Error del servidor al eliminar el cargo")
                }
            } else {
                showSyncAlert("Sin conexión, cargo eliminado localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting cargo: ${e.message}")
            showSyncAlert("Error al eliminar: ${e.message}")
            throw e
        }
    }

    override suspend fun deleteAllCargos() {
        val errorList = mutableListOf<String>()
        var hasLocalChanges = false

        try {
            val localCargos = cargoDao.getAllCargos().first()
            cargoDao.deleteAllCargos()
            hasLocalChanges = true

            if (isNetworkAvailable()) {
                val serverResponse = cargoApiService.getCargos()
                if (!serverResponse.isSuccessful) {
                    throw Exception("Error al obtener cargos del servidor")
                }

                val serverCargos = serverResponse.body()?.filterNotNull() ?: emptyList()
                val allCargos = (serverCargos.map { it.id } + localCargos.mapNotNull { it.id }).distinct()

                allCargos.forEach { cargoId ->
                    cargoId?.let {
                        val response = cargoApiService.deleteCargo(it)
                        if (!response.isSuccessful) {
                            val error = "Error al eliminar cargo $it: ${response.errorBody()?.string()}"
                            Log.e(TAG, error)
                            errorList.add(error)
                        }
                    }
                }

                if (errorList.isNotEmpty()) {
                    throw Exception("Errores al eliminar cargos en el servidor:\n${errorList.joinToString("\n")}")
                }

                showSyncAlert("Todos los cargos eliminados correctamente")
            } else {
                showSyncAlert("Sin conexión, cargos eliminados localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in deleteAllCargos: ${e.message}")
            val message = if (hasLocalChanges) {
                "Cargos eliminados localmente, pero hubo errores en el servidor: ${e.message}"
            } else {
                "Error al eliminar cargos: ${e.message}"
            }
            showSyncAlert(message)
            throw Exception(message)
        }
    }

    override suspend fun syncCargos() {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "No connection available for sync")
            showSyncAlert("Sin conexión, usando datos locales")
            return
        }

        try {
            val response = cargoApiService.getCargos()
            if (!response.isSuccessful) {
                throw Exception("Error al obtener cargos del servidor")
            }

            val serverCargos = response.body()?.filterNotNull() ?: emptyList()
            val localCargos = cargoDao.getAllCargos().first()
            val serverCargosMap = serverCargos.associateBy { it.id }
            val localCargosMap = localCargos.associateBy { it.id }

            serverCargos.forEach { serverCargo ->
                val localCargo = localCargosMap[serverCargo.id]
                val domainCargo = CargoMapper.fromResponse(serverCargo)

                if (localCargo == null) {
                    cargoDao.insertCargo(CargoMapper.toDatabase(domainCargo))
                } else {
                    cargoDao.updateCargo(CargoMapper.toDatabase(domainCargo))
                }
            }

            localCargos.filter { it.id == null || !serverCargosMap.containsKey(it.id) }
                .forEach { localCargo ->
                    try {
                        val domainCargo = CargoMapper.toDomain(localCargo)
                        val createResponse = cargoApiService.createCargo(CargoMapper.toRequest(domainCargo))

                        if (createResponse.isSuccessful && createResponse.body() != null) {
                            val newServerCargo = CargoMapper.fromResponse(createResponse.body()!!)
                            cargoDao.updateCargo(CargoMapper.toDatabase(newServerCargo))
                        } else {
                            logServerError(createResponse, "Error syncing local cargo")
                            throw Exception("Error al sincronizar cargo local con el servidor")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing local cargo: ${e.message}")
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
            val response = cargoApiService.getCargos()
            if (!response.isSuccessful) {
                throw Exception("Error al obtener datos del servidor")
            }

            val serverCargos = response.body()?.filterNotNull() ?: emptyList()
            val localCargos = cargoDao.getAllCargos().first()

            localCargos.filter { it.id == null }.forEach { localCargo ->
                val createResponse = cargoApiService.createCargo(
                    CargoMapper.toRequest(CargoMapper.toDomain(localCargo))
                )
                if (!createResponse.isSuccessful) {
                    throw Exception("Error al sincronizar cargo local")
                }
            }

            cargoDao.deleteAllCargos()
            serverCargos.forEach { serverCargo ->
                cargoDao.insertCargo(CargoMapper.toDatabase(CargoMapper.fromResponse(serverCargo)))
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