package com.agrojurado.sfmappv2.data.sync

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.agrojurado.sfmappv2.data.remote.dto.common.utils.Utils
import com.agrojurado.sfmappv2.domain.repository.LoteRepository
import com.agrojurado.sfmappv2.domain.repository.FincaRepository
import com.agrojurado.sfmappv2.domain.repository.AreaRepository
import com.agrojurado.sfmappv2.domain.repository.CargoRepository
import com.agrojurado.sfmappv2.domain.repository.OperarioRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSyncManager @Inject constructor(
    private val loteRepository: LoteRepository,
    private val fincaRepository: FincaRepository,
    private val areaRepository: AreaRepository,
    private val cargoRepository: CargoRepository,
    private val operarioRepository: OperarioRepository,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DataSyncManager"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private fun isNetworkAvailable(): Boolean {
        return Utils.isNetworkAvailable(context)
    }

    fun syncAllData(onSyncComplete: () -> Unit) {
        if (!isNetworkAvailable()) {
            Toast.makeText(context, "Sin Conexión a Internet", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "No hay conexión a Internet disponible para sincronización")
            onSyncComplete() // Asegúrate de llamar al callback incluso si no hay conexión
            return
        }

        coroutineScope.launch {
            try {
                val syncJobs = listOf(
                    async { syncLotes() },
                    async { syncFincas() },
                    async { syncAreas() },
                    async { syncCargos() },
                    async { syncOperarios() }
                )

                syncJobs.awaitAll()
                Log.d(TAG, "Sincronización completa de todos los datos")
            } catch (e: Exception) {
                Log.e(TAG, "Error durante la sincronización: ${e.message}")
            } finally {
                onSyncComplete() // Llama al callback al finalizar
            }
        }
    }

    fun autoSyncOnReconnect() {
        coroutineScope.launch {
            while (true) {
                delay(5000) // Verificar cada 5 segundos
                if (isNetworkAvailable()) {
                    syncAllData {}
                    break // Salir del bucle una vez sincronizado
                }
            }
        }
    }

    private suspend fun syncLotes() {
        try {
            loteRepository.syncLotes()
            Log.d(TAG, "Sincronización de lotes completada")
        } catch (e: Exception) {
            Log.e(TAG, "Error sincronizando lotes: ${e.message}")
        }
    }

    private suspend fun syncFincas() {
        try {
            fincaRepository.syncFincas()
            Log.d(TAG, "Sincronización de fincas completada")
        } catch (e: Exception) {
            Log.e(TAG, "Error sincronizando fincas: ${e.message}")
        }
    }

    private suspend fun syncAreas() {
        try {
            areaRepository.syncAreas()
            Log.d(TAG, "Sincronización de áreas completada")
        } catch (e: Exception) {
            Log.e(TAG, "Error sincronizando áreas: ${e.message}")
        }
    }

    private suspend fun syncCargos() {
        try {
            cargoRepository.syncCargos()
            Log.d(TAG, "Sincronización de cargos completada")
        } catch (e: Exception) {
            Log.e(TAG, "Error sincronizando cargos: ${e.message}")
        }
    }

    private suspend fun syncOperarios() {
        try {
            operarioRepository.syncOperarios()
            Log.d(TAG, "Sincronización de operarios completada")
        } catch (e: Exception) {
            Log.e(TAG, "Error sincronizando operarios: ${e.message}")
        }
    }
}
