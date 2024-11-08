package com.agrojurado.sfmappv2.data.repository

import android.content.Context
import android.util.Log
import com.agrojurado.sfmappv2.data.local.dao.LoteDao
import com.agrojurado.sfmappv2.data.mapper.LoteMapper
import com.agrojurado.sfmappv2.data.remote.api.LoteApiService
import com.agrojurado.sfmappv2.domain.model.Lote
import com.agrojurado.sfmappv2.domain.repository.LoteRepository
import com.agrojurado.sfmappv2.data.remote.dto.common.utils.Utils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LoteRepositoryImpl @Inject constructor(
    private val loteDao: LoteDao,
    private val loteApiService: LoteApiService,
    @ApplicationContext private val context: Context
) : LoteRepository {

    companion object {
        private const val TAG = "LoteRepository"
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

    override fun getAllLotes(): Flow<List<Lote>> {
        return loteDao.getAllLotes().map { entities ->
            entities.map(LoteMapper::toDomain)
        }
    }

    override suspend fun getLoteById(id: Int): Lote? {
        return loteDao.getLoteById(id)?.let(LoteMapper::toDomain)
    }

    override suspend fun insertLote(lote: Lote): Long {
        var localId = 0L
        try {
            localId = loteDao.insertLote(LoteMapper.toDatabase(lote))

            if (isNetworkAvailable()) {
                val loteRequest = LoteMapper.toRequest(lote.copy(id = localId.toInt()))
                val response = loteApiService.createLote(loteRequest)

                if (response.isSuccessful && response.body() != null) {
                    val serverLote = LoteMapper.fromResponse(response.body()!!)
                    loteDao.updateLote(LoteMapper.toDatabase(serverLote))
                    return serverLote.id?.toLong() ?: localId
                } else {
                    logServerError(response, "Error creando el lote en el servidor")
                    throw Exception("Error del servidor al crear el lote")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creando lote: ${e.message}")

            if (localId == 0L) {
                throw Exception("Error al guardar el lote: ${e.message}")
            }
        }

        if (!isNetworkAvailable() && localId != 0L) {
            showSyncAlert("Lote guardado localmente, se sincronizará cuando haya conexión")
        }
        return localId
    }

    override suspend fun updateLote(lote: Lote) {
        try {
            loteDao.updateLote(LoteMapper.toDatabase(lote))
            if (isNetworkAvailable() && lote.id != null) {
                val response = loteApiService.updateLote(LoteMapper.toRequest(lote))

                if (!response.isSuccessful) {
                    logServerError(response, "Error actualizando lote en el servidor")
                    throw Exception("Error del servidor al actualizar el lote")
                }
            } else {
                showSyncAlert("Sin conexión, lote actualizado localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando lote: ${e.message}")
            showSyncAlert("Error al actualizar: ${e.message}")
            throw e
        }
    }

    override suspend fun deleteLote(lote: Lote) {
        try {
            loteDao.deleteLote(LoteMapper.toDatabase(lote))
            if (isNetworkAvailable() && lote.id != null) {
                val response = loteApiService.deleteLote(lote.id)
                if (!response.isSuccessful) {
                    logServerError(response, "Error eliminando lote en el servidor")
                    throw Exception("Error del servidor al eliminar el lote")
                }
            } else {
                showSyncAlert("Sin conexión, lote eliminado localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando lote: ${e.message}")
            showSyncAlert("Error al eliminar: ${e.message}")
            throw e
        }
    }

    override suspend fun deleteAllLotes() {
        val errorList = mutableListOf<String>()
        var hasLocalChanges = false

        try {
            val localLotes = loteDao.getAllLotes().first()
            loteDao.deleteAllLotes()
            hasLocalChanges = true

            if (isNetworkAvailable()) {
                val serverResponse = loteApiService.getLotes()
                if (!serverResponse.isSuccessful) {
                    throw Exception("Error al obtener lotes del servidor")
                }

                val serverLotes = serverResponse.body()?.filterNotNull() ?: emptyList()
                val allLotes = (serverLotes.map { it.id } + localLotes.mapNotNull { it.id }).distinct()

                allLotes.forEach { loteId ->
                    loteId?.let {
                        val response = loteApiService.deleteLote(it)
                        if (!response.isSuccessful) {
                            val error = "Error al eliminar lote $it: ${response.errorBody()?.string()}"
                            Log.e(TAG, error)
                            errorList.add(error)
                        }
                    }
                }

                if (errorList.isNotEmpty()) {
                    throw Exception("Errores al eliminar lotes en el servidor:\n${errorList.joinToString("\n")}")
                }

                showSyncAlert("Todos los lotes eliminados correctamente")
            } else {
                showSyncAlert("Sin conexión, lotes eliminados localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en deleteAllLotes: ${e.message}")
            val message = if (hasLocalChanges) {
                "Lotes eliminados localmente, pero hubo errores en el servidor: ${e.message}"
            } else {
                "Error al eliminar lotes: ${e.message}"
            }
            showSyncAlert(message)
            throw Exception(message)
        }
    }

    override suspend fun syncLotes() {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "No connection available for synchronization")
            showSyncAlert("No connection, using local data")
            return
        }

        try {
            val response = loteApiService.getLotes()
            if (!response.isSuccessful) {
                if (response.code() == 404) {
                    Log.w(TAG, "No se encontraron lotes en el servidor.")
                    return // O notificar al usuario que no hay datos
                }
                throw Exception("Error getting lotes from the server")
            }

            val serverLotes = response.body()?.filterNotNull() ?: emptyList()
            val localLotes = loteDao.getAllLotes().first()

            if (localLotes.isEmpty()) {
                // Si la base de datos local está vacía, insertamos todos los lotes del servidor
                serverLotes.forEach { serverLote ->
                    val domainLote = LoteMapper.fromResponse(serverLote)
                    loteDao.insertLote(LoteMapper.toDatabase(domainLote))
                }
                showSyncAlert("Sincronización completada")
                return
            }

            // Lógica de sincronización
            val serverLotesMap = serverLotes.associateBy { it.id }
            val localLotesMap = localLotes.associateBy { it.id }

            serverLotes.forEach { serverLote ->
                val localLote = localLotesMap[serverLote.id]
                val domainLote = LoteMapper.fromResponse(serverLote)

                if (localLote == null) {
                    loteDao.insertLote(LoteMapper.toDatabase(domainLote))
                } else {
                    loteDao.updateLote(LoteMapper.toDatabase(domainLote))
                }
            }

            localLotes.filter { it.id == null || !serverLotesMap.containsKey(it.id) }
                .forEach { localLote ->
                    try {
                        val domainLote = LoteMapper.toDomain(localLote)
                        val createResponse = loteApiService.createLote(LoteMapper.toRequest(domainLote))

                        if (createResponse.isSuccessful && createResponse.body() != null) {
                            val newServerLote = LoteMapper.fromResponse(createResponse.body()!!)
                            loteDao.updateLote(LoteMapper.toDatabase(newServerLote))
                        } else {
                            logServerError(createResponse, "Error synchronizing local lote")
                            throw Exception("Error synchronizing local lote with the server")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error synchronizing local lote: ${e.message}")
                    }
                }

            showSyncAlert("Sincronización completada")
        } catch (e: Exception) {
            Log.e(TAG, "Synchronization error: ${e.message}")
            throw Exception("Synchronization error: ${e.message}")
        }
    }

    override suspend fun fullSync(): Boolean {
        if (!isNetworkAvailable()) {
            showSyncAlert("Sin conexión a Internet")
            return false
        }

        try {
            val response = loteApiService.getLotes()
            if (!response.isSuccessful) {
                if (response.code() == 404) {
                    Log.w(TAG, "No se encontraron lotes en el servidor.")
                    return true // Considerar que la sincronización se completó sin errores
                }
                throw Exception("Error al obtener datos del servidor")
            }

            val serverLotes = response.body()?.filterNotNull() ?: emptyList()
            val localLotes = loteDao.getAllLotes().first()

            // Si la base de datos local está vacía, insertamos los lotes del servidor
            if (localLotes.isEmpty()) {
                serverLotes.forEach { serverLote ->
                    loteDao.insertLote(LoteMapper.toDatabase(LoteMapper.fromResponse(serverLote)))
                }
            } else {
                // Aquí va la lógica de sincronización si la base de datos no está vacía
                localLotes.forEach { localLote ->
                    // Lógica de sincronización entre local y servidor
                }
            }

            showSyncAlert("Sincronización completada")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error de sincronización completa: ${e.message}")
            throw Exception("Error en la sincronización completa: ${e.message}")
        }
    }
}
