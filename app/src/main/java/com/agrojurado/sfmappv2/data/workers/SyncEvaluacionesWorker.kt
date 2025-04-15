package com.agrojurado.sfmappv2.data.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.agrojurado.sfmappv2.data.repository.EvaluacionGeneralRepositoryImpl
import com.agrojurado.sfmappv2.data.repository.EvaluacionPolinizacionRepositoryImpl
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SyncEvaluacionesWorker @Inject constructor(
    @ApplicationContext context: Context,
    params: WorkerParameters,
    private val evaluacionPolinizacionRepository: EvaluacionPolinizacionRepositoryImpl,
    private val evaluacionGeneralRepository: EvaluacionGeneralRepositoryImpl
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncEvaluacionesWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting sync of EvaluacionGeneral and associated EvaluacionPolinizacion")
            evaluacionGeneralRepository.syncEvaluacionesGenerales()
            Log.d(TAG, "Sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}", e)
            Result.retry() // Reintentar en caso de fallo
        }
    }
}