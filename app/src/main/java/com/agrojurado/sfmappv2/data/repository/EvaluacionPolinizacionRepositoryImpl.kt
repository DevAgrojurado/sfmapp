package com.agrojurado.sfmappv2.data.repository

import android.content.Context
import android.util.Log
import com.agrojurado.sfmappv2.data.local.dao.EvaluacionPolinizacionDao
import com.agrojurado.sfmappv2.data.mapper.EvaluacionPolinizacionMapper
import com.agrojurado.sfmappv2.data.remote.api.EvaluacionApiService
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import com.agrojurado.sfmappv2.domain.repository.EvaluacionPolinizacionRepository
import com.agrojurado.sfmappv2.data.remote.dto.common.utils.Utils
import com.agrojurado.sfmappv2.data.remote.dto.evaluacion.EvaluacionResponse
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.domain.repository.OperarioRepository
import com.agrojurado.sfmappv2.domain.repository.UsuarioRepository
import com.agrojurado.sfmappv2.domain.security.UserRoleConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class EvaluacionPolinizacionRepositoryImpl @Inject constructor(
    private val dao: EvaluacionPolinizacionDao,
    private val apiService: EvaluacionApiService,
    private val usuarioRepository: UsuarioRepository,
    private val operarioRepository: OperarioRepository,
    @ApplicationContext private val context: Context
) : EvaluacionPolinizacionRepository {

    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val syncMutex = Mutex()

    companion object {
        private const val TAG = "EvaluacionRepository"
    }

    private fun isNetworkAvailable(): Boolean = Utils.isNetworkAvailable(context)

    private fun showSyncAlert(message: String) {
        // Asegurarse de que el Toast se ejecute en el hilo principal
        CoroutineScope(Dispatchers.Main).launch {
            Utils.showAlert(context, message)
        }
    }

    private fun logServerError(response: retrofit2.Response<*>, logMessage: String) {
        Utils.logError(TAG, Exception("Server error (${response.code()}): ${response.errorBody()?.string()}"), logMessage)
    }

    override fun getEvaluaciones(): Flow<List<EvaluacionPolinizacion>> {
        return dao.getEvaluaciones().map { entities ->
            entities.map(EvaluacionPolinizacionMapper::toDomain)
        }
    }

    override suspend fun getEvaluacionById(id: Int): EvaluacionPolinizacion? {
        return dao.getEvaluacionById(id)?.let(EvaluacionPolinizacionMapper::toDomain)
    }

    override suspend fun getLastEvaluacion(): EvaluacionPolinizacion? {
        return dao.getLastEvaluacion()?.let(EvaluacionPolinizacionMapper::toDomain)
    }

    override suspend fun checkPalmExists(semana: Int, lote: Int, palma: Int, idPolinizador: Int): Boolean {
        return dao.checkPalmExists(semana, lote, palma, idPolinizador) > 0
    }

    override suspend fun insertEvaluacion(evaluacion: EvaluacionPolinizacion): Long {
        // Primero guardamos localmente de manera inmediata
        val localId = withContext(Dispatchers.IO) {
            dao.insertEvaluacion(
                EvaluacionPolinizacionMapper.toDatabase(
                    evaluacion.copy(
                        isSynced = false,
                        timestamp = System.currentTimeMillis()
                    )
                )
            )
        }

        return localId
    }


    override suspend fun updateEvaluacion(evaluacion: EvaluacionPolinizacion) {
        // Actualizar inmediatamente en local
        withContext(Dispatchers.IO) {
            dao.updateEvaluacion(
                EvaluacionPolinizacionMapper.toDatabase(evaluacion)
                    .apply { isSynced = false }
            )
        }

        // Sincronizar en background
        syncScope.launch {
            try {
                if (isNetworkAvailable()) {
                    syncMutex.withLock {
                        try {
                            val evaluacionRequest = EvaluacionPolinizacionMapper.toRequest(evaluacion)
                            val response = apiService.updateEvaluacion(evaluacion.id!!, evaluacionRequest)

                            if (response.isSuccessful) {
                                dao.updateEvaluacion(
                                    EvaluacionPolinizacionMapper.toDatabase(evaluacion)
                                        .apply { isSynced = true }
                                )
                                withContext(Dispatchers.Main) {
                                    Utils.showToast(context, "Actualización sincronizada")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error en sincronización background: ${e.message}")
                            withContext(Dispatchers.Main) {
                                Utils.showToast(context, "La actualización se sincronizará cuando haya conexión")
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Utils.showToast(context, "Actualización guardada localmente")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en proceso background: ${e.message}")
            }
        }
    }



    override suspend fun deleteEvaluacion(evaluacion: EvaluacionPolinizacion) {
        try {
            // Eliminar localmente primero
            dao.deleteEvaluacion(EvaluacionPolinizacionMapper.toDatabase(evaluacion))

            // Si hay conexión y tenemos el ID del servidor, eliminamos también en el servidor
            if (isNetworkAvailable()) {
                evaluacion.serverId?.let { serverId ->
                    val response = apiService.deleteEvaluacion(serverId)
                    if (response.isSuccessful) {
                        Log.d(TAG, "Evaluación eliminada correctamente en el servidor")
                        withContext(Dispatchers.Main) {
                            Utils.showToast(context, "Evaluación eliminada en el servidor")
                        }
                    } else {
                        logServerError(response, "Error al eliminar evaluación en el servidor")
                        // Aunque falle en el servidor, ya se eliminó localmente
                        withContext(Dispatchers.Main) {
                            Utils.showToast(context, "Error al eliminar en el servidor, pero se eliminó localmente")
                        }
                    }
                } ?: run {
                    // No tenía ID del servidor, solo se eliminó localmente
                    withContext(Dispatchers.Main) {
                        Utils.showToast(context, "Evaluación eliminada localmente")
                    }
                }
            } else {
                // Sin conexión, solo eliminamos localmente
                withContext(Dispatchers.Main) {
                    Utils.showToast(context, "Sin conexión, evaluación eliminada localmente")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando evaluación: ${e.message}")
            withContext(Dispatchers.Main) {
                Utils.showToast(context, "Error al eliminar: ${e.message}")
            }
            throw e
        }
    }

    override suspend fun deleteAllEvaluaciones() {
        val errorList = mutableListOf<String>()
        var hasLocalChanges = false

        try {
            // Eliminar todas las evaluaciones locales
            dao.deleteAllEvaluaciones()
            hasLocalChanges = true
            Log.d(TAG, "Evaluaciones locales eliminadas.")

            if (isNetworkAvailable()) {
                // Eliminar del servidor las evaluaciones que están localmente
                val response = apiService.getEvaluaciones()
                if (!response.isSuccessful) {
                    throw Exception("Error al obtener evaluaciones del servidor")
                }

                val serverEvaluaciones = response.body()?.filterNotNull() ?: emptyList()

                serverEvaluaciones.forEach { serverEvaluacion ->
                    try {
                        val response = apiService.deleteEvaluacion(serverEvaluacion.id)
                        if (!response.isSuccessful) {
                            val error = "Error al eliminar evaluación ${serverEvaluacion.id}: ${response.errorBody()?.string()}"
                            Log.e(TAG, error)
                            errorList.add(error)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al eliminar evaluación ${serverEvaluacion.id}: ${e.message}")
                        errorList.add("Error al eliminar evaluación ${serverEvaluacion.id}: ${e.message}")
                    }
                }

                if (errorList.isNotEmpty()) {
                    throw Exception("Errores al eliminar evaluación en el servidor:\n${errorList.joinToString("\n")}")
                }

                showSyncAlert("Todas las evaluaciones eliminadas correctamente del servidor")
            } else {
                // Sin conexión, solo eliminamos localmente
                showSyncAlert("Sin conexión, evaluaciones eliminadas localmente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar todas las evaluaciones: ${e.message}")
            val message = if (hasLocalChanges) {
                "Evaluaciones eliminadas localmente, pero hubo errores en el servidor: ${e.message}"
            } else {
                "Error al eliminar evaluaciones: ${e.message}"
            }
            showSyncAlert(message)
            throw Exception(message)
        }
    }

    override suspend fun syncEvaluaciones() = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            withContext(Dispatchers.Main) {
                showSyncAlert("Sin conexión, usando datos locales")
            }
            return@withContext
        }

        val currentUser = usuarioRepository.getLoggedInUserEmail()?.let { email ->
            usuarioRepository.getUserByEmail(email).first()
        }

        syncMutex.withLock {
            // First, handle local unsynced evaluaciones
            val unsyncedEvaluaciones = dao.getEvaluaciones().first().filter { !it.isSynced }

            for (localEvaluacion in unsyncedEvaluaciones) {
                try {
                    val evaluacionRequest = EvaluacionPolinizacionMapper.toRequest(
                        EvaluacionPolinizacionMapper.toDomain(localEvaluacion)
                    )

                    val response = if (localEvaluacion.id != null) {
                        val checkResponse = apiService.getEvaluacionById(localEvaluacion.id)
                        if (checkResponse.isSuccessful && checkResponse.body() != null) {
                            // La evaluación existe en el servidor
                            if (checkResponse.body()!!.timestamp < localEvaluacion.timestamp) {
                                // Local es más reciente, actualizar servidor
                                apiService.updateEvaluacion(localEvaluacion.id, evaluacionRequest)
                            } else {
                                // Servidor es más reciente, mantener versión del servidor
                                checkResponse
                            }
                        } else {
                            // No existe en el servidor, crear nueva
                            apiService.createEvaluacion(evaluacionRequest)
                        }
                    } else {
                        // No tiene ID, crear nueva
                        apiService.createEvaluacion(evaluacionRequest)
                    }

                    if (response.isSuccessful && response.body() != null) {
                        val serverEvaluacion = response.body()!!
                        dao.updateEvaluacion(localEvaluacion.copy(
                            id = serverEvaluacion.id,
                            isSynced = true,
                            timestamp = serverEvaluacion.timestamp
                        ))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sincronizando evaluación local: ${e.message}")
                }
            }

            // Then, get all server evaluaciones
            try {
                val serverResponse = apiService.getEvaluaciones()
                if (!serverResponse.isSuccessful) {
                    throw Exception("Error obteniendo evaluaciones del servidor")
                }

                val serverEvaluaciones = serverResponse.body()?.filterNotNull() ?: emptyList()
                val filteredServerEvaluaciones = filterEvaluacionesByUserRole(serverEvaluaciones, currentUser)

                val localEvaluacionesMap = dao.getEvaluaciones().first().associateBy { it.id }

                dao.transaction {
                    // Delete local evaluaciones not in server
                    localEvaluacionesMap.values
                        .filter { it.id != null && !filteredServerEvaluaciones.any { se -> se.id == it.id } }
                        .forEach { dao.deleteEvaluacion(it) }

                    // Update or insert server evaluaciones
                    filteredServerEvaluaciones.forEach { serverEvaluacion ->
                        val domainEvaluacion = EvaluacionPolinizacionMapper.fromResponse(serverEvaluacion)
                        val localEvaluacion = localEvaluacionesMap[serverEvaluacion.id]

                        if (localEvaluacion == null || !localEvaluacion.isSynced) {
                            // Insert if doesn't exist or update if not synced
                            dao.insertEvaluacion(
                                EvaluacionPolinizacionMapper.toDatabase(domainEvaluacion)
                                    .apply { isSynced = true }
                            )
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    showSyncAlert("Sincronización completada exitosamente")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en sincronización: ${e.message}")
                withContext(Dispatchers.Main) {
                    showSyncAlert("Error durante la sincronización: ${e.message}")
                }
            }
        }
    }

    // Helper function to filter evaluaciones based on user role
    private fun filterEvaluacionesByUserRole(
        serverEvaluaciones: List<EvaluacionResponse>,
        currentUser: Usuario?
    ): List<EvaluacionResponse> {
        return currentUser?.let { user ->
            when {
                user.rol.equals(UserRoleConstants.ROLE_ADMIN, ignoreCase = true) ||
                        user.rol.equals(UserRoleConstants.ROLE_COORDINATOR, ignoreCase = true) ->
                    serverEvaluaciones
                user.rol.equals(UserRoleConstants.ROLE_EVALUATOR, ignoreCase = true) -> {
                    val operariosEnFinca = runBlocking {
                        operarioRepository.getAllOperarios()
                            .first()
                            .filter { it.fincaId == user.idFinca }
                            .map { it.id }
                    }
                    serverEvaluaciones.filter { operariosEnFinca.contains(it.idpolinizador) }
                }
                else -> emptyList()
            }
        } ?: serverEvaluaciones
    }


}