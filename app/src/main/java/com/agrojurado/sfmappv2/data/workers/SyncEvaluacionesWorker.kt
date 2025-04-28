package com.agrojurado.sfmappv2.data.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.agrojurado.sfmappv2.domain.repository.EvaluacionGeneralRepository
import com.agrojurado.sfmappv2.domain.repository.EvaluacionPolinizacionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncEvaluacionesWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val evaluacionPolinizacionRepository: EvaluacionPolinizacionRepository,
    private val evaluacionGeneralRepository: EvaluacionGeneralRepository
) : CoroutineWorker(appContext, workerParams) {

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