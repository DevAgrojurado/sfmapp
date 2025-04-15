package com.agrojurado.sfmappv2.data.repository

import android.content.Context
import android.util.Log
import com.agrojurado.sfmappv2.data.local.dao.CargoDao
import com.agrojurado.sfmappv2.data.mapper.CargoMapper
import com.agrojurado.sfmappv2.data.remote.api.CargoApiService
import com.agrojurado.sfmappv2.domain.model.Cargo
import com.agrojurado.sfmappv2.domain.repository.CargoRepository
import com.agrojurado.sfmappv2.data.remote.dto.common.utils.Utils
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
        return Utils.isNetworkAvailable(context)
    }

    private fun showSyncAlert(message: String) {
        Utils.showAlert(context, message)
    }

    private fun logServerError(response: retrofit2.Response<*>, logMessage: String) {
        Utils.logError(TAG, Exception("Server error (${response.code()}): ${response.errorBody()?.string()}"), logMessage)
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
        try {
            if (isNetworkAvailable()) {
                val cargoRequest = CargoMapper.toRequest(cargo)
                val response = cargoApiService.createCargo(cargoRequest)

                syncCargos()

                if (response.isSuccessful && response.body() != null) {
                    val serverCargo = CargoMapper.fromResponse(response.body()!!)
                    val localId = cargoDao.insertCargo(CargoMapper.toDatabase(serverCargo))
                    return localId
                } else {
                    logServerError(response, "Error creando el cargo en el servidor")
                    throw Exception("Error del servidor al crear el cargo")
                }
            } else {
                val localId = cargoDao.insertCargo(CargoMapper.toDatabase(cargo))
                showSyncAlert("Cargo guardado localmente, se sincronizará cuando haya conexión")
                return localId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creando cargo: ${e.message}")
            throw Exception("Error al guardar el cargo: ${e.message}")
        }
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

                serverCargos.forEach { serverCargo ->
                    try {
                        val response = cargoApiService.deleteCargo(serverCargo.id)
                        if (!response.isSuccessful) {
                            val error = "Error al eliminar cargo ${serverCargo.id}: ${response.errorBody()?.string()}"
                            Log.e(TAG, error)
                            errorList.add(error)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al eliminar cargo en el servidor: ${e.message}")
                        errorList.add("Error al eliminar cargo ${serverCargo.id} en el servidor: ${e.message}")
                    }
                }

                if (errorList.isNotEmpty()) {
                    throw Exception("Errores al eliminar cargos en el servidor:\n${errorList.joinToString("\n")}")
                }

                showSyncAlert("Todos los cargos eliminados correctamente del servidor")
            } else {
                showSyncAlert("Sin conexión, cargos eliminados localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar todos los cargos: ${e.message}")
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
            Log.d(TAG, "No hay conexión disponible para la sincronización")
            throw Exception("Sin conexión, usando datos locales") // Lanzar excepción en lugar de mostrar alerta
        }

        try {
            val response = cargoApiService.getCargos()
            if (!response.isSuccessful) {
                if (response.code() == 404) {
                    Log.w(TAG, "No se encontraron cargos en el servidor.")
                    return // Si no hay cargos en el servidor, no es un error crítico
                }
                logServerError(response, "Error al obtener cargos del servidor")
                throw Exception("Error al obtener cargos del servidor: ${response.code()}")
            }

            val serverCargos = response.body()?.filterNotNull() ?: emptyList()
            val localCargos = cargoDao.getAllCargos().first()
            val serverCargosMap = serverCargos.associateBy { it.id }
            val localCargosMap = localCargos.associateBy { it.id }

            // Paso 1: Actualizar o insertar cargos del servidor que ya existen localmente
            serverCargos.forEach { serverCargo ->
                val localCargo = localCargosMap[serverCargo.id]
                val domainCargo = CargoMapper.fromResponse(serverCargo)

                if (localCargo == null) {
                    cargoDao.insertCargo(CargoMapper.toDatabase(domainCargo))
                } else {
                    cargoDao.updateCargo(CargoMapper.toDatabase(domainCargo))
                }
            }

            // Paso 2: Sincronizar cargos locales que no existan en el servidor
            localCargos.filter { it.id == null || !serverCargosMap.containsKey(it.id) }
                .forEach { localCargo ->
                    try {
                        // Verifica si el cargo local ya existe en el servidor antes de crearlo
                        if (!serverCargosMap.containsKey(localCargo.id)) {
                            val domainCargo = CargoMapper.toDomain(localCargo)
                            val createResponse = cargoApiService.createCargo(CargoMapper.toRequest(domainCargo))

                            if (createResponse.isSuccessful && createResponse.body() != null) {
                                val newServerCargo = CargoMapper.fromResponse(createResponse.body()!!)
                                cargoDao.updateCargo(CargoMapper.toDatabase(newServerCargo)) // Actualiza el cargo local con el nuevo ID
                            } else {
                                logServerError(createResponse, "Error sincronizando cargo local")
                                throw Exception("Error al sincronizar cargo local con el servidor: ${createResponse.errorBody()?.string()}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sincronizando cargo local: ${e.message}")
                        throw e // Re-lanzar para manejo superior
                    }
                }

            Log.d(TAG, "Sincronización de cargos completada exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error de sincronización: ${e.message}")
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
                if (response.code() == 404) {
                    Log.w(TAG, "No se encontraron cargos en el servidor.")
                    cargoDao.deleteAllCargos() // Asegurarse de limpiar también localmente
                    return true
                }
                throw Exception("Error al obtener datos del servidor")
            }

            val serverCargos = response.body()?.filterNotNull() ?: emptyList()
            cargoDao.deleteAllCargos() // Limpia la base de datos local antes de la nueva inserción
            serverCargos.forEach { serverCargo ->
                cargoDao.insertCargo(CargoMapper.toDatabase(CargoMapper.fromResponse(serverCargo)))
            }

            showSyncAlert("Sincronización completada")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la sincronización completa: ${e.message}")
            throw Exception("Error en la sincronización completa: ${e.message}")
        }
    }
}