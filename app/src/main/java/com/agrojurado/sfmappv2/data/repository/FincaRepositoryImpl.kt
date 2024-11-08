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

    private fun isNetworkAvailable(): Boolean {
        return Utils.isNetworkAvailable(context)
    }

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
        var localId = 0L
        try {
            localId = fincaDao.insertFinca(FincaMapper.toDatabase(finca))

            if (isNetworkAvailable()) {
                val fincaRequest = FincaMapper.toRequest(finca.copy(id = localId.toInt()))
                val response = fincaApiService.createFinca(fincaRequest)

                if (response.isSuccessful && response.body() != null) {
                    val serverFinca = FincaMapper.fromResponse(response.body()!!)
                    fincaDao.updateFinca(FincaMapper.toDatabase(serverFinca))
                    return serverFinca.id?.toLong() ?: localId
                } else {
                    logServerError(response, "Error creando la finca en el servidor")
                    throw Exception("Error del servidor al crear la finca")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creando finca: ${e.message}")
            if (localId == 0L) {
                throw Exception("Error al guardar la finca: ${e.message}")
            }
        }

        if (!isNetworkAvailable() && localId != 0L) {
            showSyncAlert("Finca guardada localmente, se sincronizará cuando haya conexión")
        }
        return localId
    }

    override suspend fun updateFinca(finca: Finca) {
        try {
            fincaDao.updateFinca(FincaMapper.toDatabase(finca))
            if (isNetworkAvailable() && finca.id != null) {
                val response = fincaApiService.updateFinca(FincaMapper.toRequest(finca))

                if (!response.isSuccessful) {
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
                if (!response.isSuccessful) {
                    logServerError(response, "Error eliminando finca en el servidor")
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
            val localFincas = fincaDao.getAllFincas().first()
            fincaDao.deleteAllFincas()
            hasLocalChanges = true

            if (isNetworkAvailable()) {
                val serverResponse = fincaApiService.getFincas()
                if (!serverResponse.isSuccessful) {
                    throw Exception("Error al obtener fincas del servidor")
                }

                val serverFincas = serverResponse.body()?.filterNotNull() ?: emptyList()
                val allFincas = (serverFincas.map { it.id } + localFincas.mapNotNull { it.id }).distinct()

                allFincas.forEach { fincaId ->
                    fincaId?.let {
                        val response = fincaApiService.deleteFinca(it)
                        if (!response.isSuccessful) {
                            val error = "Error al eliminar finca $it: ${response.errorBody()?.string()}"
                            Log.e(TAG, error)
                            errorList.add(error)
                        }
                    }
                }

                if (errorList.isNotEmpty()) {
                    throw Exception("Errores al eliminar fincas en el servidor:\n${errorList.joinToString("\n")}")
                }

                showSyncAlert("Todas las fincas eliminadas correctamente")
            } else {
                showSyncAlert("Sin conexión, fincas eliminadas localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en deleteAllFincas: ${e.message}")
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
            val response = fincaApiService.getFincas()
            if (!response.isSuccessful) {
                throw Exception("Error al obtener fincas del servidor")
            }

            val serverFincas = response.body()?.filterNotNull() ?: emptyList()
            val localFincas = fincaDao.getAllFincas().first()
            val serverFincasMap = serverFincas.associateBy { it.id }
            val localFincasMap = localFincas.associateBy { it.id }

            serverFincas.forEach { serverFinca ->
                val localFinca = localFincasMap[serverFinca.id]
                val domainFinca = FincaMapper.fromResponse(serverFinca)

                if (localFinca == null) {
                    fincaDao.insertFinca(FincaMapper.toDatabase(domainFinca))
                } else {
                    fincaDao.updateFinca(FincaMapper.toDatabase(domainFinca))
                }
            }

            localFincas.filter { it.id == null || !serverFincasMap.containsKey(it.id) }
                .forEach { localFinca ->
                    try {
                        val domainFinca = FincaMapper.toDomain(localFinca)
                        val createResponse = fincaApiService.createFinca(FincaMapper.toRequest(domainFinca))

                        if (createResponse.isSuccessful && createResponse.body() != null) {
                            val newServerFinca = FincaMapper.fromResponse(createResponse.body()!!)
                            fincaDao.updateFinca(FincaMapper.toDatabase(newServerFinca))
                        } else {
                            logServerError(createResponse, "Error sincronizando finca local")
                            throw Exception("Error al sincronizar finca local con el servidor")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sincronizando finca local: ${e.message}")
                    }
                }

            showSyncAlert("Sincronización completada")
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
            val response = fincaApiService.getFincas()
            if (!response.isSuccessful) {
                throw Exception("Error al obtener datos del servidor")
            }

            val serverFincas = response.body()?.filterNotNull() ?: emptyList()
            val localFincas = fincaDao.getAllFincas().first()

            // Insertar cargos locales que no tienen ID
            localFincas.filter { it.id == null }.forEach { localFinca ->
                val createResponse = fincaApiService.createFinca(
                    FincaMapper.toRequest(FincaMapper.toDomain(localFinca))
                )
                if (!createResponse.isSuccessful) {
                    throw Exception("Error al sincronizar finca local")
                }
            }

            fincaDao.deleteAllFincas()
            serverFincas.forEach { serverFinca ->
                fincaDao.insertFinca(FincaMapper.toDatabase(FincaMapper.fromResponse(serverFinca)))
            }

            showSyncAlert("Sincronización completada")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error de sincronización completa: ${e.message}")
            throw Exception("Error en la sincronización completa: ${e.message}")
        }
    }
}
