package com.agrojurado.sfmappv2.data.repository

import android.content.Context
import android.util.Log
import com.agrojurado.sfmappv2.data.local.dao.LoteDao
import com.agrojurado.sfmappv2.data.mapper.LoteMapper
import com.agrojurado.sfmappv2.data.remote.api.LoteApiService
import com.agrojurado.sfmappv2.domain.model.Lote
import com.agrojurado.sfmappv2.domain.repository.LoteRepository
import com.agrojurado.sfmappv2.data.remote.dto.common.utils.Utils
import com.agrojurado.sfmappv2.domain.repository.UsuarioRepository
import com.agrojurado.sfmappv2.domain.security.RoleAccessControl
import com.agrojurado.sfmappv2.domain.security.UserRoleConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class LoteRepositoryImpl @Inject constructor(
    private val loteDao: LoteDao,
    private val loteApiService: LoteApiService,
    private val usuarioRepository: UsuarioRepository,
    private val roleAccessControl: RoleAccessControl,
    @ApplicationContext private val context: Context
) : LoteRepository {

    companion object {
        private const val TAG = "LoteRepository"
    }

    private fun isNetworkAvailable(): Boolean = Utils.isNetworkAvailable(context)

    private fun showSyncAlert(message: String) {
        Utils.showAlert(context, message)
    }

    private fun logServerError(response: retrofit2.Response<*>, logMessage: String) {
        Utils.logError(TAG, Exception("Server error (${response.code()}): ${response.errorBody()?.string()}"), logMessage)
    }

    override fun getAllLotes(): Flow<List<Lote>> {
        return loteDao.getAllLotes().map { entities ->
            val domainLotes = entities.map(LoteMapper::toDomain)

            // Get current logged-in user
            val currentUser = usuarioRepository.getLoggedInUserEmail()?.let { email ->
                runBlocking {
                    usuarioRepository.getUserByEmail(email).first()
                }
            }

            // If user exists, filter lots based on their role
            currentUser?.let { user ->
                roleAccessControl.filterLotsForUser(user, domainLotes)
            } ?: domainLotes
        }
    }

    override suspend fun getLoteById(id: Int): Lote? {
        return loteDao.getLoteById(id)?.let(LoteMapper::toDomain)
    }

    override suspend fun insertLote(lote: Lote): Long {
        try {
            if (isNetworkAvailable()) {
                val loteRequest = LoteMapper.toRequest(lote)
                val response = loteApiService.createLote(loteRequest)

                if (response.isSuccessful && response.body() != null) {
                    val serverLote = LoteMapper.fromResponse(response.body()!!)
                    val loteWithServerId = lote.copy(id = serverLote.id, isSynced = true)
                    return loteDao.insertLote(LoteMapper.toDatabase(loteWithServerId))
                } else {
                    logServerError(response, "Error creando el lote en el servidor")
                    throw Exception("Error del servidor al crear el lote")
                }
            } else {
                val localLote = lote.copy(id = 0, isSynced = false)
                val localId = loteDao.insertLote(LoteMapper.toDatabase(localLote))
                showSyncAlert("Guardado localmente, se sincronizará cuando haya conexión")
                return localId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creando lote: ${e.message}")
            throw Exception("Error al guardar el lote: ${e.message}")
        }
    }

    override suspend fun updateLote(lote: Lote) {
        try {
            loteDao.updateLote(LoteMapper.toDatabase(lote).apply { isSynced = false })

            if (isNetworkAvailable() && lote.id != null) {
                val response = loteApiService.updateLote(LoteMapper.toRequest(lote))
                if (response.isSuccessful) {
                    loteDao.updateLote(LoteMapper.toDatabase(lote).apply { isSynced = true })
                } else {
                    logServerError(response, "Error actualizando el lote en el servidor")
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
                if (response.isSuccessful) {
                    Log.d(TAG, "Lote eliminado correctamente en el servidor")
                } else {
                    logServerError(response, "Error al eliminar lote en el servidor")
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
            loteDao.deleteAllLotes()
            hasLocalChanges = true
            Log.d(TAG, "Datos locales eliminados")

            if (isNetworkAvailable()) {
                val serverResponse = loteApiService.getLotes()
                if (!serverResponse.isSuccessful) {
                    throw Exception("Error al obtener lotes del servidor")
                }

                val serverLotes = serverResponse.body()?.filterNotNull() ?: emptyList()
                serverLotes.forEach { serverLote ->
                    try {
                        val response = loteApiService.deleteLote(serverLote.id)
                        if (!response.isSuccessful) {
                            val error = "Error al eliminar lote ${serverLote.id}: ${response.errorBody()?.string()}"
                            Log.e(TAG, error)
                            errorList.add(error)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al eliminar lote en el servidor: ${e.message}")
                        errorList.add("Error al eliminar lote ${serverLote.id} en el servidor: ${e.message}")
                    }
                }

                if (errorList.isNotEmpty()) {
                    throw Exception("Errores al eliminar lotes en el servidor:\n${errorList.joinToString("\n")}")
                }
                showSyncAlert("Todos los lotes eliminados correctamente del servidor")
            } else {
                showSyncAlert("Sin conexión, lotes eliminados localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar todos los lotes: ${e.message}")
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
            Log.d(TAG, "No hay conexión disponible para la sincronización")
            throw Exception("Sin conexión, usando datos locales") // Lanzar excepción en lugar de mostrar alerta
        }

        // Obtener el usuario actual
        val currentUser = usuarioRepository.getLoggedInUserEmail()?.let { email ->
            usuarioRepository.getUserByEmail(email).first()
        }

        try {
            // Obtener lotes del servidor
            val serverResponse = currentUser?.idFinca?.let {
                loteApiService.getLotesByFinca(it)
            } ?: loteApiService.getLotes()

            if (!serverResponse.isSuccessful) {
                logServerError(serverResponse, "Error al obtener lotes del servidor")
                throw Exception("Error al obtener lotes del servidor: ${serverResponse.code()}")
            }

            val serverLotes = serverResponse.body()?.filterNotNull() ?: emptyList()

            // Filtrar lotes si es un evaluador
            val filteredServerLotes = currentUser?.let { user ->
                if (roleAccessControl.hasRole(user, UserRoleConstants.ROLE_EVALUATOR)) {
                    user.idFinca?.let { farmId ->
                        serverLotes.filter { it.idFinca == farmId }
                    } ?: emptyList()
                } else {
                    serverLotes
                }
            } ?: serverLotes

            // Sincronizar lotes locales no sincronizados
            val unsyncedLotes = loteDao.getAllLotes().first().filter { !it.isSynced }

            if (unsyncedLotes.isNotEmpty()) {
                Log.d(TAG, "Lotes no sincronizados encontrados: ${unsyncedLotes.size}")
                unsyncedLotes.forEach { localLote ->
                    try {
                        // Verificar si este lote ya existe en el servidor
                        if (localLote.id != null && filteredServerLotes.any { it.id == localLote.id }) {
                            loteDao.updateLote(localLote.copy(isSynced = true))
                            Log.d(TAG, "Lote ${localLote.id} ya existe en el servidor, marcado como sincronizado")
                            return@forEach
                        }

                        val loteRequest = LoteMapper.toRequest(LoteMapper.toDomain(localLote))

                        if (loteRequest.id == null) {
                            Log.e(TAG, "Campos obligatorios faltantes para el lote: ${localLote.id}")
                            throw Exception("Campos obligatorios faltantes para el lote: ${localLote.id}")
                        }

                        val response = loteApiService.createLote(loteRequest)
                        if (response.isSuccessful && response.body() != null) {
                            val serverLote = LoteMapper.fromResponse(response.body()!!)
                            val updatedLote = localLote.copy(id = serverLote.id, isSynced = true)
                            loteDao.updateLote(updatedLote)
                            Log.d(TAG, "Lote sincronizado correctamente con el ID: ${updatedLote.id}")
                        } else {
                            logServerError(response, "Error al sincronizar lote ${localLote.id}")
                            throw Exception("Error al sincronizar lote ${localLote.id}: ${response.errorBody()?.string()}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al sincronizar lote ${localLote.id}: ${e.message}")
                        throw e // Re-lanzar para que se maneje arriba
                    }
                }
            }

            // Actualizar la base de datos local con los lotes del servidor
            loteDao.transaction {
                val localLotesMap = loteDao.getAllLotes().first().associateBy { it.id }

                filteredServerLotes.forEach { serverLote ->
                    val domainLote = LoteMapper.fromResponse(serverLote)
                    val localLote = localLotesMap[serverLote.id]

                    if (localLote == null) {
                        loteDao.insertLote(LoteMapper.toDatabase(domainLote).apply { isSynced = true })
                    } else if (!localLote.isSynced) {
                        loteDao.updateLote(LoteMapper.toDatabase(domainLote).apply { isSynced = true })
                    }
                }
            }

            Log.d(TAG, "Sincronización de lotes completada exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la sincronización de lotes: ${e.message}")
            throw e // Lanzar la excepción para que DataSyncManager la capture
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
                logServerError(response, "Error en la sincronización completa")
                return false
            }

            val serverLotes = response.body()?.filterNotNull() ?: emptyList()

            loteDao.transaction {
                val localLotesMap = loteDao.getAllLotes().first().associateBy { it.id }

                serverLotes.forEach { serverLote ->
                    val domainLote = LoteMapper.fromResponse(serverLote)
                    val localLote = localLotesMap[serverLote.id]

                    if (localLote == null) {
                        loteDao.insertLote(LoteMapper.toDatabase(domainLote).apply { isSynced = true })
                        Log.d(TAG, "Lote insertado desde el servidor: ${domainLote.id}")
                    } else {
                        loteDao.updateLote(LoteMapper.toDatabase(domainLote).apply { isSynced = true })
                        Log.d(TAG, "Lote actualizado desde el servidor: ${domainLote.id}")
                    }
                }

                localLotesMap.values
                    .filter { localLote ->
                        !serverLotes.any { it.id == localLote.id }
                    }
                    .forEach { loteToDelete ->
                        try {
                            loteDao.deleteLote(loteToDelete)
                            Log.d(TAG, "Lote eliminado: ${loteToDelete.id}")
                        } catch (e: Exception) {
                            Log.w(TAG, "No se pudo eliminar el lote ${loteToDelete.id} porque tiene referencias asociadas")
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