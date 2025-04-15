package com.agrojurado.sfmappv2.data.repository

import android.content.Context
import android.util.Log
import com.agrojurado.sfmappv2.data.local.dao.FincaDao
import com.agrojurado.sfmappv2.data.mapper.FincaMapper
import com.agrojurado.sfmappv2.data.remote.api.FincaApiService
import com.agrojurado.sfmappv2.domain.model.Finca
import com.agrojurado.sfmappv2.domain.repository.FincaRepository
import com.agrojurado.sfmappv2.data.remote.dto.common.utils.Utils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FincaRepositoryImpl @Inject constructor(
    private val fincaDao: FincaDao,
    private val fincaApiService: FincaApiService,
    @ApplicationContext private val context: Context
) : FincaRepository {

    companion object {
        private const val TAG = "FincaRepository"
    }

    private fun isNetworkAvailable(): Boolean = Utils.isNetworkAvailable(context)

    private fun showSyncAlert(message: String) {
        Utils.showAlert(context, message)
    }

    private fun logServerError(response: retrofit2.Response<*>, logMessage: String) {
        Utils.logError(TAG, Exception("Server error (${response.code()}): ${response.errorBody()?.string()}"), logMessage)
    }

    override fun getAllFincas(): Flow<List<Finca>> {
        return fincaDao.getAllFincas().map { entities ->
            entities.map(FincaMapper::toDomain)
        }
    }

    override suspend fun getFincaById(id: Int): Finca? {
        return fincaDao.getFincaById(id)?.let(FincaMapper::toDomain)
    }

    override suspend fun insertFinca(finca: Finca): Long {
        try {
            if (isNetworkAvailable()) {
                val fincaRequest = FincaMapper.toRequest(finca)
                val response = fincaApiService.createFinca(fincaRequest)
                syncFincas()

                if (response.isSuccessful && response.body() != null) {
                    val serverFinca = FincaMapper.fromResponse(response.body()!!)
                    val fincaWithServerId = finca.copy(id = serverFinca.id, isSynced = true)
                    return fincaDao.insertFinca(FincaMapper.toDatabase(fincaWithServerId))
                } else {
                    logServerError(response, "Error creando la finca en el servidor")
                    throw Exception("Error del servidor al crear la finca")
                }
            } else {
                val localFinca = finca.copy(id = 0, isSynced = false)
                val localId = fincaDao.insertFinca(FincaMapper.toDatabase(localFinca))
                showSyncAlert("Guardado localmente, se sincronizará cuando haya conexión")
                return localId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creando finca: ${e.message}")
            throw Exception("Error al guardar la finca: ${e.message}")
        }
    }

    override suspend fun updateFinca(finca: Finca) {
        try {
            fincaDao.updateFinca(FincaMapper.toDatabase(finca).apply { isSynced = false })

            if (isNetworkAvailable() && finca.id != null) {
                val response = fincaApiService.updateFinca(FincaMapper.toRequest(finca))
                if (response.isSuccessful) {
                    fincaDao.updateFinca(FincaMapper.toDatabase(finca).apply { isSynced = true })
                } else {
                    logServerError(response, "Error actualizando la finca en el servidor")
                    throw Exception("Error del servidor al actualizar la finca")
                }
            } else {
                showSyncAlert("Sin conexión, finca actualizada localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando finca: ${e.message}")
            showSyncAlert("Error al actualizar: ${e.message}")
            throw e
        }
    }

    override suspend fun deleteFinca(finca: Finca) {
        try {
            fincaDao.deleteFinca(FincaMapper.toDatabase(finca))

            if (isNetworkAvailable() && finca.id != null) {
                val response = fincaApiService.deleteFinca(finca.id)
                if (response.isSuccessful) {
                    Log.d(TAG, "Finca eliminada correctamente en el servidor")
                } else {
                    logServerError(response, "Error al eliminar finca en el servidor")
                    throw Exception("Error del servidor al eliminar la finca")
                }
            } else {
                showSyncAlert("Sin conexión, finca eliminada localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando finca: ${e.message}")
            showSyncAlert("Error al eliminar: ${e.message}")
            throw e
        }
    }

    override suspend fun deleteAllFincas() {
        val errorList = mutableListOf<String>()
        var hasLocalChanges = false

        try {
            fincaDao.deleteAllFincas()
            hasLocalChanges = true
            Log.d(TAG, "Datos locales eliminados")

            if (isNetworkAvailable()) {
                val serverResponse = fincaApiService.getFincas()
                if (!serverResponse.isSuccessful) {
                    throw Exception("Error al obtener fincas del servidor")
                }

                val serverFincas = serverResponse.body()?.filterNotNull() ?: emptyList()
                serverFincas.forEach { serverFinca ->
                    try {
                        val response = fincaApiService.deleteFinca(serverFinca.id)
                        if (!response.isSuccessful) {
                            val error = "Error al eliminar finca ${serverFinca.id}: ${response.errorBody()?.string()}"
                            Log.e(TAG, error)
                            errorList.add(error)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al eliminar finca en el servidor: ${e.message}")
                        errorList.add("Error al eliminar finca ${serverFinca.id} en el servidor: ${e.message}")
                    }
                }

                if (errorList.isNotEmpty()) {
                    throw Exception("Errores al eliminar fincas en el servidor:\n${errorList.joinToString("\n")}")
                }
                showSyncAlert("Todas las fincas eliminadas correctamente del servidor")
            } else {
                showSyncAlert("Sin conexión, fincas eliminadas localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar todas las fincas: ${e.message}")
            val message = if (hasLocalChanges) {
                "Fincas eliminadas localmente, pero hubo errores en el servidor: ${e.message}"
            } else {
                "Error al eliminar fincas: ${e.message}"
            }
            showSyncAlert(message)
            throw Exception(message)
        }
    }

    override suspend fun syncFincas() {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "No hay conexión disponible para la sincronización")
            showSyncAlert("Sin conexión, usando datos locales")
            return
        }

        try {
            val unsyncedFincas = fincaDao.getAllFincas().first().filter { !it.isSynced }

            if (unsyncedFincas.isNotEmpty()) {
                Log.d(TAG, "Fincas no sincronizadas encontradas: ${unsyncedFincas.size}")
                unsyncedFincas.forEach { localFinca ->
                    try {
                        val fincaRequest = FincaMapper.toRequest(FincaMapper.toDomain(localFinca))

                        if (fincaRequest.id == null) {
                            Log.e(TAG, "Campos obligatorios faltantes para la finca: ${localFinca.id}")
                            return@forEach
                        }

                        val response = fincaApiService.createFinca(fincaRequest)
                        if (response.isSuccessful && response.body() != null) {
                            val serverFinca = FincaMapper.fromResponse(response.body()!!)
                            val updatedFinca = localFinca.copy(id = serverFinca.id, isSynced = true)
                            fincaDao.updateFinca(updatedFinca)
                            Log.d(TAG, "Finca sincronizada correctamente con el ID: ${updatedFinca.id}")
                        } else {
                            Log.e(TAG, "Error al sincronizar finca ${localFinca.id}: ${response.errorBody()?.string()}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al sincronizar finca ${localFinca.id}: ${e.message}")
                    }
                }
            }

            val response = fincaApiService.getFincas()
            if (!response.isSuccessful) {
                logServerError(response, "Error obteniendo fincas del servidor")
                throw Exception("Error al obtener fincas del servidor")
            }

            val serverFincas = response.body()?.filterNotNull() ?: emptyList()
            if (serverFincas.isEmpty()) {
                Log.d(TAG, "El servidor no tiene fincas, no se realiza la sincronización.")
                showSyncAlert("El servidor no tiene fincas para sincronizar.")
                return
            }

            val localFincasMap = fincaDao.getAllFincas().first().associateBy { it.id }

            localFincasMap.values.filter { it.id != null && !serverFincas.any { serverFinca -> serverFinca.id == it.id } }
                .forEach { localFinca ->
                    fincaDao.deleteFinca(localFinca)
                    Log.d(TAG, "Finca eliminada localmente porque no existe en el servidor: ${localFinca.id}")
                }

            serverFincas.forEach { serverFinca ->
                val domainFinca = FincaMapper.fromResponse(serverFinca)
                val localFinca = localFincasMap[serverFinca.id]
                if (localFinca == null) {
                    fincaDao.insertFinca(FincaMapper.toDatabase(domainFinca).apply { isSynced = true })
                } else {
                    fincaDao.updateFinca(FincaMapper.toDatabase(domainFinca).apply { isSynced = true })
                }
            }

            Log.d(TAG, "Sincronización completa")
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
            val response = fincaApiService.getFincas()
            if (!response.isSuccessful) {
                logServerError(response, "Error en la sincronización completa")
                return false
            }

            val serverFincas = response.body()?.filterNotNull() ?: emptyList()

            fincaDao.transaction {
                val localFincasMap = fincaDao.getAllFincas().first().associateBy { it.id }

                serverFincas.forEach { serverFinca ->
                    val domainFinca = FincaMapper.fromResponse(serverFinca)
                    val localFinca = localFincasMap[serverFinca.id]

                    if (localFinca == null) {
                        fincaDao.insertFinca(FincaMapper.toDatabase(domainFinca).apply { isSynced = true })
                        Log.d(TAG, "Finca insertada desde el servidor: ${domainFinca.id}")
                    } else {
                        fincaDao.updateFinca(FincaMapper.toDatabase(domainFinca).apply { isSynced = true })
                        Log.d(TAG, "Finca actualizada desde el servidor: ${domainFinca.id}")
                    }
                }

                localFincasMap.values
                    .filter { localFinca ->
                        !serverFincas.any { it.id == localFinca.id }
                    }
                    .forEach { fincaToDelete ->
                        try {
                            fincaDao.deleteFinca(fincaToDelete)
                            Log.d(TAG, "Finca eliminada: ${fincaToDelete.id}")
                        } catch (e: Exception) {
                            Log.w(TAG, "No se pudo eliminar la finca ${fincaToDelete.id} porque tiene referencias asociadas")
                        }
                    }
            }

            Log.d(TAG, "Sincronización completa exitosa.")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error en la sincronización completa: ${e.message}", e)
            showSyncAlert("Error durante la sincronización completa: ${e.message}")
            return false
        }
    }
}