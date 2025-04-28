package com.agrojurado.sfmappv2.data.sync

import android.content.Context
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import com.agrojurado.sfmappv2.data.remote.dto.common.utils.Utils
import com.agrojurado.sfmappv2.domain.repository.*
import com.agrojurado.sfmappv2.utils.SyncNotificationManager
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
    private val notificationManager = SyncNotificationManager.getInstance(context)

    private fun isNetworkAvailable(): Boolean {
        return Utils.isNetworkAvailable(context)
    }

    fun syncAllData(progressBar: ProgressBar, onSyncComplete: () -> Unit) {
        if (!isNetworkAvailable()) {
            coroutineScope.launch {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Sin Conexión a Internet", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "No hay conexión a Internet disponible para sincronización")
                    progressBar.visibility = ProgressBar.GONE
                    onSyncComplete()
                }
            }
            return
        }

        // Crear notificación inicial
        notificationManager.startSyncNotification(
            "Sincronizando datos",
            "Preparando sincronización..."
        )

        coroutineScope.launch {
            try {
                // Mostrar mensaje inicial en hilo principal
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.VISIBLE
                    progressBar.progress = 0
                }

                // Definir las operaciones de sincronización respetando las dependencias de claves foráneas
                val syncOperations = listOf<suspend (Int, Int) -> Unit>(
                    // Entidades base (sin dependencias)
                    { index, total -> syncData("fincas", progressBar, index, total) { fincaRepository.syncFincas() } },
                    { index, total -> syncData("áreas", progressBar, index, total) { areaRepository.syncAreas() } },
                    { index, total -> syncData("cargos", progressBar, index, total) { cargoRepository.syncCargos() } },

                    // Entidades con dependencias de Finca, Área y Cargo
                    { index, total -> syncData("lotes", progressBar, index, total) { loteRepository.syncLotes() } },
                    { index, total -> syncData("operarios", progressBar, index, total) { operarioRepository.syncOperarios() } },
                    { index, total -> syncData("usuarios", progressBar, index, total) { usuarioRepository.syncUsuarios() } },

                    // Entidades de evaluaciones (incluye EvaluacionPolinizacion)
                    { index, total -> syncData("evaluaciones generales", progressBar, index, total) { evaluacionGeneralRepository.syncEvaluacionesGenerales() } },
                    
                    // Explícitamente sincronizar las evaluaciones de polinización
                    { index, total -> syncData("evaluaciones de polinización", progressBar, index, total) { 
                        try {
                            evaluacionPolinizacionRepository.fetchEvaluacionesFromServer()
                            Log.d(TAG, "✅ Sincronización explícita de evaluaciones de polinización completada")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error en sincronización explícita de evaluaciones de polinización: ${e.message}", e)
                            throw e
                        }
                    }}
                )

                val totalOperations = syncOperations.size
                for ((index, syncOperation) in syncOperations.withIndex()) {
                    syncOperation(index, totalOperations)
                    withContext(Dispatchers.Main) {
                        progressBar.progress = ((index + 1) * 100) / totalOperations
                    }
                }

                // Actualizar notificación final
                notificationManager.completeSyncNotification("Sincronización completa de todos los datos")
                Log.d(TAG, "Sincronización completa de todos los datos")

                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.GONE
                    onSyncComplete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en proceso de sincronización: ${e.message}", e)
                notificationManager.errorSyncNotification("Error en sincronización: ${e.message}")

                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.GONE
                    Toast.makeText(context, "Error en sincronización: ${e.message}", Toast.LENGTH_LONG).show()
                    onSyncComplete()
                }
            }
        }
    }

    // Método auxiliar para ejecutar una operación de sincronización individual
    private suspend fun syncData(
        dataType: String,
        progressBar: ProgressBar,
        currentIndex: Int,
        totalOperations: Int,
        syncOperation: suspend () -> Unit
    ) {
        try {
            withContext(Dispatchers.Main) {
                progressBar.isIndeterminate = true
            }
            
            // Actualizar notificación con operación actual
            val progress = ((currentIndex * 100) / totalOperations)
            notificationManager.updateSyncProgress(
                progress,
                100,
                "Sincronizando $dataType..."
            )
            
            syncOperation()
            
            withContext(Dispatchers.Main) {
                progressBar.isIndeterminate = false
            }
            
            // Actualizar notificación con la operación completada
            notificationManager.updateSyncProgress(
                ((currentIndex + 1) * 100) / totalOperations,
                100,
                "Completada sincronización de $dataType"
            )
            
            Log.d(TAG, "Sincronización de $dataType completada")
        } catch (e: Exception) {
            Log.e(TAG, "Error sincronizando $dataType: ${e.message}", e)
            notificationManager.updateSyncMessage("Error al sincronizar $dataType")
            throw e // Relanzar la excepción para manejarla en el bloque superior
        }
    }

    fun autoSyncOnReconnect() {
        coroutineScope.launch {
            while (true) {
                delay(5000) // Verificar cada 5 segundos
                if (isNetworkAvailable()) {
                    try {
                        // Crear un ProgressBar dummy que no se muestra
                        val dummyProgressBar = ProgressBar(context).apply { visibility = ProgressBar.GONE }
                        
                        // Iniciar notificación
                        notificationManager.startSyncNotification(
                            "Sincronización automática",
                            "Iniciando sincronización en segundo plano..."
                        )
                        
                        // Realizar sincronización en segundo plano
                        syncAllData(dummyProgressBar) {}
                        
                        break // Salir del bucle una vez sincronizado
                    } catch (e: Exception) {
                        Log.e(TAG, "Error en sincronización automática: ${e.message}", e)
                        notificationManager.errorSyncNotification("Error en sincronización automática: ${e.message}")
                        delay(30000) // Esperar más tiempo después de un error
                    }
                }
            }
        }
    }
}