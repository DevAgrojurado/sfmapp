package com.agrojurado.sfmappv2.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import com.agrojurado.sfmappv2.data.local.dao.LoteDao
import com.agrojurado.sfmappv2.data.mapper.LoteMapper
import com.agrojurado.sfmappv2.data.remote.api.LoteApiService
import com.agrojurado.sfmappv2.domain.model.Lote
import com.agrojurado.sfmappv2.domain.repository.LoteRepository
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
            // Guardar el lote localmente y obtener el ID local
            localId = loteDao.insertLote(LoteMapper.toDatabase(lote))

            // Intentar sincronizar si hay conexión de red
            if (isNetworkAvailable()) {
                val loteRequest = LoteMapper.toRequest(lote.copy(id = localId.toInt()))
                val response = loteApiService.createLote(loteRequest)

                if (response.isSuccessful && response.body() != null) {
                    // Actualizar con los datos del servidor si la sincronización fue exitosa
                    val serverLote = LoteMapper.fromResponse(response.body()!!)
                    loteDao.updateLote(LoteMapper.toDatabase(serverLote))
                    return serverLote.id?.toLong() ?: localId
                } else {
                    logServerError(response, "Error creando el lote en el servidor")
                    throw Exception("Error del servidor al crear el lote")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating lote: ${e.message}")

            // Si no se guardó localmente, lanzar excepción
            if (localId == 0L) {
                throw Exception("Error al guardar el lote: ${e.message}")
            }
        }

        // Mostrar alerta solo si no hay conexión y se guardó localmente
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
                    logServerError(response, "Error updating lote on server")
                    throw Exception("Error del servidor al actualizar el lote")
                }
            } else {
                showSyncAlert("Sin conexión, lote actualizado localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating lote: ${e.message}")
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
                    logServerError(response, "Error deleting lote on server")
                    throw Exception("Error del servidor al eliminar el lote")
                }
            } else {
                showSyncAlert("Sin conexión, lote eliminado localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting lote: ${e.message}")
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
            Log.e(TAG, "Error in deleteAllLotes: ${e.message}")
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
            Log.d(TAG, "No connection available for sync")
            showSyncAlert("Sin conexión, usando datos locales")
            return
        }

        try {
            // Obtener todos los lotes desde el servidor
            val response = loteApiService.getLotes()
            if (!response.isSuccessful) {
                throw Exception("Error al obtener lotes del servidor")
            }

            val serverLotes = response.body()?.filterNotNull() ?: emptyList()
            val localLotes = loteDao.getAllLotes().first()
            val serverLotesMap = serverLotes.associateBy { it.id }
            val localLotesMap = localLotes.associateBy { it.id }

            // Sincronizar lotes desde el servidor
            serverLotes.forEach { serverLote ->
                val localLote = localLotesMap[serverLote.id]
                val domainLote = LoteMapper.fromResponse(serverLote)

                if (localLote == null) {
                    loteDao.insertLote(LoteMapper.toDatabase(domainLote))
                } else {
                    loteDao.updateLote(LoteMapper.toDatabase(domainLote))
                }
            }

            // Sincronizar lotes locales que no están en el servidor
            localLotes.filter { it.id == null || !serverLotesMap.containsKey(it.id) }
                .forEach { localLote ->
                    try {
                        val domainLote = LoteMapper.toDomain(localLote)
                        val createResponse = loteApiService.createLote(LoteMapper.toRequest(domainLote))

                        if (createResponse.isSuccessful && createResponse.body() != null) {
                            val newServerLote = LoteMapper.fromResponse(createResponse.body()!!)
                            loteDao.updateLote(LoteMapper.toDatabase(newServerLote))
                        } else {
                            logServerError(createResponse, "Error syncing local lote")
                            throw Exception("Error al sincronizar lote local con el servidor")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing local lote: ${e.message}")
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
            val response = loteApiService.getLotes()
            if (!response.isSuccessful) {
                throw Exception("Error al obtener datos del servidor")
            }

            val serverLotes = response.body()?.filterNotNull() ?: emptyList()
            val localLotes = loteDao.getAllLotes().first()

            // Sincronizar lotes locales sin ID (nuevos)
            localLotes.filter { it.id == null }.forEach { localLote ->
                val createResponse = loteApiService.createLote(
                    LoteMapper.toRequest(LoteMapper.toDomain(localLote))
                )
                if (!createResponse.isSuccessful) {
                    throw Exception("Error al sincronizar lote local")
                }
            }

            // Limpiar base de datos local y actualizar con datos del servidor
            loteDao.deleteAllLotes()
            serverLotes.forEach { serverLote ->
                loteDao.insertLote(LoteMapper.toDatabase(LoteMapper.fromResponse(serverLote)))
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