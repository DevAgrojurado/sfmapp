package com.agrojurado.sfmappv2.data.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.agrojurado.sfmappv2.data.repository.EvaluacionGeneralRepositoryImpl
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import android.util.Log
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncWorkerEntryPoint {
        fun evaluacionGeneralRepository(): EvaluacionGeneralRepositoryImpl
    }

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "SyncWorker iniciado en ${System.currentTimeMillis()}")

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SyncWorkerEntryPoint::class.java
        )
        val repository = entryPoint.evaluacionGeneralRepository()

        return try {
            Log.d("SyncWorker", "Iniciando sincronización de evaluaciones generales...")
            repository.syncEvaluacionesGenerales()
            Log.d("SyncWorker", "Sincronización completada con éxito")
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error durante la sincronización: ${e.message}", e)
            Result.retry() // Reintentar si falla
        }
    }
}

// Función para programar el worker
fun scheduleSync(context: Context) {
    Log.d("SyncWorker", "Programando trabajo de sincronización...")
    val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(
        repeatInterval = 15,
        repeatIntervalTimeUnit = TimeUnit.MINUTES
    )
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()

    WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork(
            "evaluacion_sync",
            ExistingPeriodicWorkPolicy.KEEP, // Mantener el trabajo existente si ya está programado
            workRequest
        )
    Log.d("SyncWorker", "Trabajo de sincronización programado correctamente")
}