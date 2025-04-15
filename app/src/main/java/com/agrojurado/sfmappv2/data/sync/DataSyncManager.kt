package com.agrojurado.sfmappv2.data.sync

import android.content.Context
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import com.agrojurado.sfmappv2.data.remote.dto.common.utils.Utils
import com.agrojurado.sfmappv2.domain.repository.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSyncManager @Inject constructor(
    private val loteRepository: LoteRepository,
    private val fincaRepository: FincaRepository,
    private val areaRepository: AreaRepository,
    private val cargoRepository: CargoRepository,
    private val operarioRepository: OperarioRepository,
    private val usuarioRepository: UsuarioRepository,
    private val evaluacionPolinizacionRepository: EvaluacionPolinizacionRepository,
    private val evaluacionGeneralRepository: EvaluacionGeneralRepository,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DataSyncManager"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private fun isNetworkAvailable(): Boolean {
        return Utils.isNetworkAvailable(context)
    }

    fun syncAllData(progressBar: ProgressBar, onSyncComplete: () -> Unit) {
        if (!isNetworkAvailable()) {
            Toast.makeText(context, "Sin Conexión a Internet", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "No hay conexión a Internet disponible para sincronización")
            onSyncComplete()
            return
        }

        coroutineScope.launch {
            try {
                // Definir las operaciones de sincronización respetando las dependencias de claves foráneas
                val syncOperations = listOf<suspend () -> Unit>(
                    // Entidades base (sin dependencias)
                    { syncData("fincas", progressBar) { fincaRepository.syncFincas() } },
                    { syncData("áreas", progressBar) { areaRepository.syncAreas() } },
                    { syncData("cargos", progressBar) { cargoRepository.syncCargos() } },

                    // Entidades con dependencias de Finca, Área y Cargo
                    { syncData("lotes", progressBar) { loteRepository.syncLotes() } },
                    { syncData("operarios", progressBar) { operarioRepository.syncOperarios() } },
                    { syncData("usuarios", progressBar) { usuarioRepository.syncUsuarios() } },

                    // Entidades de evaluaciones (incluye EvaluacionPolinizacion)
                    { syncData("evaluaciones generales", progressBar) { evaluacionGeneralRepository.syncEvaluacionesGenerales() } }
                )

                val totalOperations = syncOperations.size
                for ((index, syncOperation) in syncOperations.withIndex()) {
                    syncOperation()
                    withContext(Dispatchers.Main) {
                        progressBar.progress = ((index + 1) * 100) / totalOperations
                        progressBar.visibility = ProgressBar.VISIBLE
                    }
                }

                Log.d(TAG, "Sincronización completa de todos los datos")
            } catch (e: Exception) {
                Log.e(TAG, "Error durante la sincronización: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error en sincronización: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.GONE
                }
                onSyncComplete()
            }
        }
    }

    private suspend fun syncData(dataType: String, progressBar: ProgressBar, syncOperation: suspend () -> Unit) {
        try {
            syncOperation()
            Log.d(TAG, "Sincronización de $dataType completada")
        } catch (e: Exception) {
            Log.e(TAG, "Error sincronizando $dataType: ${e.message}", e)
            throw e // Relanzar la excepción para manejarla en el bloque superior
        }
    }

    fun autoSyncOnReconnect() {
        coroutineScope.launch {
            while (true) {
                delay(5000) // Verificar cada 5 segundos
                if (isNetworkAvailable()) {
                    syncAllData(ProgressBar(context)) {} // Usar ProgressBar dummy si no hay UI
                    break // Salir del bucle una vez sincronizado
                }
            }
        }
    }
}