package com.agrojurado.sfmappv2.data.repository

import android.content.Context
import android.util.Log
import com.agrojurado.sfmappv2.data.local.dao.OperarioDao
import com.agrojurado.sfmappv2.data.mapper.OperarioMapper
import com.agrojurado.sfmappv2.data.remote.api.OperarioApiService
import com.agrojurado.sfmappv2.domain.model.Operario
import com.agrojurado.sfmappv2.domain.repository.OperarioRepository
import com.agrojurado.sfmappv2.data.remote.dto.common.utils.Utils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OperarioRepositoryImpl @Inject constructor(
    private val operarioDao: OperarioDao,
    private val operarioApiService: OperarioApiService,
    @ApplicationContext private val context: Context
) : OperarioRepository {

    companion object {
        private const val TAG = "OperarioRepository"
    }

    private fun isNetworkAvailable(): Boolean = Utils.isNetworkAvailable(context)

    private fun showSyncAlert(message: String) {
        Utils.showAlert(context, message)
    }

    private fun logServerError(response: retrofit2.Response<*>, logMessage: String) {
        Utils.logError(TAG, Exception("Server error (${response.code()}): ${response.errorBody()?.string()}"), logMessage)
    }

    override fun getAllOperarios(): Flow<List<Operario>> {
        return operarioDao.getAllOperarios().map { entities ->
            entities.map(OperarioMapper::toDomain)
        }
    }

    override suspend fun getOperarioById(id: Int): Operario? {
        return operarioDao.getOperarioById(id)?.let(OperarioMapper::toDomain)
    }

    override suspend fun insertOperario(operario: Operario): Long {
        try {
            if (isNetworkAvailable()) {
                val operarioRequest = OperarioMapper.toRequest(operario)
                val response = operarioApiService.createOperario(operarioRequest)
                syncOperarios()

                if (response.isSuccessful && response.body() != null) {
                    val serverOperario = OperarioMapper.fromResponse(response.body()!!)
                    val operarioWithServerId = operario.copy(id = serverOperario.id, isSynced = true)
                    return operarioDao.insertOperario(OperarioMapper.toDatabase(operarioWithServerId))
                } else {
                    logServerError(response, "Error creando el operario en el servidor")
                    throw Exception("Error del servidor al crear el operario")
                }
            } else {
                val localOperario = operario.copy(id = 0, isSynced = false)
                val localId = operarioDao.insertOperario(OperarioMapper.toDatabase(localOperario))
                showSyncAlert("Guardado localmente, se sincronizará cuando haya conexión")
                return localId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creando operario: ${e.message}")
            throw Exception("Error al guardar el operario: ${e.message}")
        }
    }

    override suspend fun updateOperario(operario: Operario) {
        try {
            operarioDao.updateOperario(OperarioMapper.toDatabase(operario).apply { isSynced = false })

            if (isNetworkAvailable() && operario.id != null) {
                val response = operarioApiService.updateOperario(OperarioMapper.toRequest(operario))
                if (response.isSuccessful) {
                    operarioDao.updateOperario(OperarioMapper.toDatabase(operario).apply { isSynced = true })
                } else {
                    logServerError(response, "Error actualizando el operario en el servidor")
                    throw Exception("Error del servidor al actualizar el operario")
                }
            } else {
                showSyncAlert("Sin conexión, operario actualizado localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando operario: ${e.message}")
            showSyncAlert("Error al actualizar: ${e.message}")
            throw e
        }
    }

    override suspend fun deleteOperario(operario: Operario) {
        try {
            operarioDao.deleteOperario(OperarioMapper.toDatabase(operario))

            if (isNetworkAvailable() && operario.id != null) {
                val response = operarioApiService.deleteOperario(operario.id)
                if (response.isSuccessful) {
                    Log.d(TAG, "Operario eliminado correctamente en el servidor")
                } else {
                    logServerError(response, "Error al eliminar operario en el servidor")
                    throw Exception("Error del servidor al eliminar el operario")
                }
            } else {
                showSyncAlert("Sin conexión, operario eliminado localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando operario: ${e.message}")
            showSyncAlert("Error al eliminar: ${e.message}")
            throw e
        }
    }

    override suspend fun deleteAllOperarios() {
        val errorList = mutableListOf<String>()
        var hasLocalChanges = false

        try {
            operarioDao.deleteAllOperarios()
            hasLocalChanges = true
            Log.d(TAG, "Datos locales eliminados")

            if (isNetworkAvailable()) {
                val serverResponse = operarioApiService.getOperarios()
                if (!serverResponse.isSuccessful) {
                    throw Exception("Error al obtener operarios del servidor")
                }

                val serverOperarios = serverResponse.body()?.filterNotNull() ?: emptyList()
                serverOperarios.forEach { serverOperario ->
                    try {
                        val response = operarioApiService.deleteOperario(serverOperario.id)
                        if (!response.isSuccessful) {
                            val error = "Error al eliminar operario ${serverOperario.id}: ${response.errorBody()?.string()}"
                            Log.e(TAG, error)
                            errorList.add(error)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al eliminar operario en el servidor: ${e.message}")
                        errorList.add("Error al eliminar operario ${serverOperario.id} en el servidor: ${e.message}")
                    }
                }

                if (errorList.isNotEmpty()) {
                    throw Exception("Errores al eliminar operarios en el servidor:\n${errorList.joinToString("\n")}")
                }
                showSyncAlert("Todos los operarios eliminados correctamente del servidor")
            } else {
                showSyncAlert("Sin conexión, operarios eliminados localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar todos los operarios: ${e.message}")
            val message = if (hasLocalChanges) {
                "Operarios eliminados localmente, pero hubo errores en el servidor: ${e.message}"
            } else {
                "Error al eliminar operarios: ${e.message}"
            }
            showSyncAlert(message)
            throw Exception(message)
        }
    }

    override suspend fun syncOperarios() {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "No hay conexión disponible para la sincronización")
            showSyncAlert("Sin conexión, usando datos locales")
            return
        }

        try {
            val unsyncedOperarios = operarioDao.getAllOperarios().first().filter { !it.isSynced }

            if (unsyncedOperarios.isNotEmpty()) {
                Log.d(TAG, "Operarios no sincronizados encontrados: ${unsyncedOperarios.size}")
                unsyncedOperarios.forEach { localOperario ->
                    try {
                        val operarioRequest = OperarioMapper.toRequest(OperarioMapper.toDomain(localOperario))

                        if (operarioRequest.id == null || operarioRequest.nombre.isNullOrBlank()) {
                            Log.e(TAG, "Campos obligatorios faltantes para el operario: ${localOperario.id}")
                            return@forEach
                        }

                        val response = operarioApiService.createOperario(operarioRequest)
                        if (response.isSuccessful && response.body() != null) {
                            val serverOperario = OperarioMapper.fromResponse(response.body()!!)
                            val updatedOperario = localOperario.copy(id = serverOperario.id, isSynced = true)
                            operarioDao.updateOperario(updatedOperario)
                            Log.d(TAG, "Operario sincronizado correctamente con el ID: ${updatedOperario.id}")
                        } else {
                            Log.e(TAG, "Error al sincronizar operario ${localOperario.id}: ${response.errorBody()?.string()}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al sincronizar operario ${localOperario.id}: ${e.message}")
                    }
                }
            }

            val response = operarioApiService.getOperarios()
            if (!response.isSuccessful) {
                logServerError(response, "Error obteniendo operarios del servidor")
                throw Exception("Error al obtener operarios del servidor")
            }

            val serverOperarios = response.body()?.filterNotNull() ?: emptyList()
            if (serverOperarios.isEmpty()) {
                Log.d(TAG, "El servidor no tiene operarios, no se realiza la sincronización.")
                showSyncAlert("El servidor no tiene operarios para sincronizar.")
                return
            }

            val localOperariosMap = operarioDao.getAllOperarios().first().associateBy { it.id }

            localOperariosMap.values.filter { it.id != null && !serverOperarios.any { serverOperario -> serverOperario.id == it.id } }
                .forEach { localOperario ->
                    operarioDao.deleteOperario(localOperario)
                    Log.d(TAG, "Operario eliminado localmente porque no existe en el servidor: ${localOperario.id}")
                }

            serverOperarios.forEach { serverOperario ->
                val domainOperario = OperarioMapper.fromResponse(serverOperario)
                val localOperario = localOperariosMap[serverOperario.id]
                if (localOperario == null) {
                    operarioDao.insertOperario(OperarioMapper.toDatabase(domainOperario).apply { isSynced = true })
                } else {
                    operarioDao.updateOperario(OperarioMapper.toDatabase(domainOperario).apply { isSynced = true })
                }
            }

            Log.d(TAG, "Sincronización completa")
            showSyncAlert("Sincronización completada exitosamente.")
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
            operarioDao.deleteAllOperarios()
            Log.d(TAG, "Operarios locales eliminados.")

            val response = operarioApiService.getOperarios()
            if (!response.isSuccessful) {
                logServerError(response, "Error en la sincronización completa")
                return false
            }

            val serverOperarios = response.body()?.filterNotNull() ?: emptyList()

            if (serverOperarios.isEmpty()) {
                Log.d(TAG, "El servidor no tiene operarios, no se realiza la sincronización completa.")
                showSyncAlert("El servidor no tiene operarios para sincronizar.")
                return true
            }

            serverOperarios.forEach { serverOperario ->
                val domainOperario = OperarioMapper.fromResponse(serverOperario)
                val localOperario = operarioDao.getOperarioById(domainOperario.id)

                if (localOperario == null) {
                    operarioDao.insertOperario(OperarioMapper.toDatabase(domainOperario).apply { isSynced = true })
                    Log.d(TAG, "Operario insertado desde el servidor: ${domainOperario.id}")
                } else {
                    operarioDao.updateOperario(OperarioMapper.toDatabase(domainOperario).apply { isSynced = true })
                    Log.d(TAG, "Operario actualizado desde el servidor: ${domainOperario.id}")
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
override fun searchOperarios(query: String): Flow<List<Operario>> {
        return operarioDao.searchOperarios(query).map { list ->
            list.map { OperarioMapper.toDomain(it) }
        }
    }
}