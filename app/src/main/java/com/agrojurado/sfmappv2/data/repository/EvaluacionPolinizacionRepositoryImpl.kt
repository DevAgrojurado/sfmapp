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
import kotlinx.coroutines.flow.flowOn
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
    private val BATCH_SIZE = 50

    companion object {
        private const val TAG = "EvaluacionRepository"
    }

    private fun isNetworkAvailable(): Boolean = Utils.isNetworkAvailable(context)

    private suspend fun notifyUser(message: String) {
        withContext(Dispatchers.Main) {
            Utils.showToast(context, message)
        }
    }

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
        return dao.getEvaluaciones()
            .map { entities -> entities.map(EvaluacionPolinizacionMapper::toDomain) }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getEvaluacionById(id: Int): EvaluacionPolinizacion? {
        return dao.getEvaluacionById(id)?.let(EvaluacionPolinizacionMapper::toDomain)
    }

    override suspend fun getLastEvaluacion(): EvaluacionPolinizacion? {
        return dao.getLastEvaluacion()?.let(EvaluacionPolinizacionMapper::toDomain)
    }

    override suspend fun checkPalmExists(semana: Int, lote: Int, palma: Int, idPolinizador: Int, seccion: Int): Boolean {
        return dao.checkPalmExists(semana, lote, palma, idPolinizador, seccion) > 0
    }

    override suspend fun insertEvaluacion(evaluacion: EvaluacionPolinizacion): Long {
        return withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()

            // Guardar localmente primero
            val localId = dao.insertEvaluacion(
                EvaluacionPolinizacionMapper.toDatabase(
                    evaluacion.copy(
                        isSynced = false,
                        timestamp = timestamp
                    )
                )
            )

            // Intentar sincronizar con el servidor en segundo plano
            syncScope.launch {
                if (isNetworkAvailable()) {
                    try {
                        val evaluacionRequest = EvaluacionPolinizacionMapper.toRequest(evaluacion)
                        val response = apiService.createEvaluacion(evaluacionRequest)

                        if (response.isSuccessful && response.body() != null) {
                            val serverEvaluacion = response.body()!!

                            // Actualizar el registro local con el ID del servidor y marcado como sincronizado
                            dao.updateEvaluacion(
                                EvaluacionPolinizacionMapper.toDatabase(
                                    evaluacion.copy(
                                        id = localId.toInt(),
                                        serverId = serverEvaluacion.id,
                                        isSynced = true,
                                        timestamp = serverEvaluacion.timestamp
                                    )
                                )
                            )
                            notifyUser("Evaluación sincronizada con el servidor")
                        } else {
                            logServerError(response, "Error al sincronizar evaluación")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al sincronizar con el servidor: ${e.message}")
                    }
                } else {
                    Log.d(TAG, "Sin conexión a Internet. Sincronización pendiente.")
                }
            }

            return@withContext localId
        }
    }



    override suspend fun updateEvaluacion(evaluacion: EvaluacionPolinizacion) {
        withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            val updatedEntity = EvaluacionPolinizacionMapper.toDatabase(evaluacion).copy(
                isSynced = false,
                timestamp = timestamp
            )
            dao.updateEvaluacion(updatedEntity)
            notifyUser("Actualización guardada localmente")
        }
    }

    override suspend fun deleteEvaluacion(evaluacion: EvaluacionPolinizacion) {
        withContext(Dispatchers.IO) {
            try {
                // Delete locally first
                dao.deleteEvaluacion(EvaluacionPolinizacionMapper.toDatabase(evaluacion))

                // Try to delete from server if we have network and a server ID
                if (isNetworkAvailable() && evaluacion.serverId != null) {
                    try {
                        val response = apiService.deleteEvaluacion(evaluacion.serverId!!)
                        if (response.isSuccessful) {
                            notifyUser("Evaluación eliminada completamente")
                        } else {
                            notifyUser("Eliminada localmente, error en servidor")
                            Log.e(TAG, "Error servidor: ${response.errorBody()?.string()}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error eliminando del servidor: ${e.message}")
                        notifyUser("Eliminada localmente, error en servidor")
                    }
                } else {
                    notifyUser("Eliminada localmente")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en eliminación: ${e.message}")
                throw e
            }
        }
    }

    override suspend fun syncEvaluaciones() {
        if (!isNetworkAvailable()) {
            notifyUser("Sin conexión, usando datos locales")
            return
        }

        withContext(Dispatchers.IO) {
            syncMutex.withLock {
                try {
                    val currentUser = usuarioRepository.getLoggedInUserEmail()?.let { email ->
                        usuarioRepository.getUserByEmail(email).first()
                    }

                    // Obtener evaluaciones del servidor
                    val serverResponse = apiService.getEvaluaciones()
                    if (!serverResponse.isSuccessful) {
                        throw Exception("Error obteniendo datos del servidor")
                    }

                    val serverEvaluaciones = serverResponse.body()?.filterNotNull() ?: emptyList()
                    val filteredEvaluaciones = filterEvaluacionesByUserRole(serverEvaluaciones, currentUser)

                    // Subir evaluaciones locales no sincronizadas
                    val unsyncedEvaluaciones = dao.getEvaluaciones().first()
                        .filter { !it.isSynced && it.serverId == null }
                        .chunked(BATCH_SIZE)

                    unsyncedEvaluaciones.forEach { batch ->
                        batch.forEach { localEvaluacion ->
                            try {
                                val evaluacionRequest = EvaluacionPolinizacionMapper.toRequest(
                                    EvaluacionPolinizacionMapper.toDomain(localEvaluacion)
                                )

                                val response = apiService.createEvaluacion(evaluacionRequest)

                                if (response.isSuccessful && response.body() != null) {
                                    val serverEvaluacion = response.body()!!
                                    // Eliminamos la versión local no sincronizada
                                    dao.deleteEvaluacionById(localEvaluacion.id)

                                    // Insertamos la versión del servidor
                                    val domainEvaluacion = EvaluacionPolinizacionMapper.fromResponse(serverEvaluacion)
                                    dao.insertEvaluacion(
                                        EvaluacionPolinizacionMapper.toDatabase(domainEvaluacion)
                                            .copy(isSynced = true)
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error sincronizando evaluación: ${e.message}")
                            }
                        }
                    }

                    // Actualizar registros sincronizados con datos del servidor
                    dao.transaction {
                        // Obtener todas las evaluaciones locales que ya están sincronizadas
                        val syncedEvaluaciones = dao.getEvaluaciones().first()
                            .filter { it.isSynced }

                        // Actualizar cada evaluación sincronizada con datos del servidor
                        syncedEvaluaciones.forEach { local ->
                            val serverEvaluacion = filteredEvaluaciones.find { it.id == local.serverId }
                            if (serverEvaluacion != null) {
                                // Actualizar con datos del servidor
                                val domainEvaluacion = EvaluacionPolinizacionMapper.fromResponse(serverEvaluacion)
                                dao.updateEvaluacion(
                                    EvaluacionPolinizacionMapper.toDatabase(domainEvaluacion)
                                        .copy(
                                            id = local.id,
                                            isSynced = true
                                        )
                                )
                            } else {
                                // Si ya no existe en el servidor, eliminar localmente
                                dao.deleteEvaluacionById(local.id)
                            }
                        }

                        // Insertar nuevas evaluaciones del servidor
                        filteredEvaluaciones
                            .filter { serverEval ->
                                syncedEvaluaciones.none { it.serverId == serverEval.id }
                            }
                            .forEach { serverEval ->
                                val domainEvaluacion = EvaluacionPolinizacionMapper.fromResponse(serverEval)
                                dao.insertEvaluacion(
                                    EvaluacionPolinizacionMapper.toDatabase(domainEvaluacion)
                                        .copy(isSynced = true)
                                )
                            }
                    }

                    notifyUser("Sincronización completada")
                } catch (e: Exception) {
                    Log.e(TAG, "Error en sincronización: ${e.message}")
                    notifyUser("Error en sincronización: ${e.message}")
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