package com.agrojurado.sfmappv2.data.workers

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

import com.agrojurado.sfmappv2.domain.repository.EvaluacionPolinizacionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltWorker
class SyncEvaluacionesWorker @Inject constructor(
    @ApplicationContext context: Context,
    workerParameters: WorkerParameters,
    private val evaluacionRepository: EvaluacionPolinizacionRepository
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        return try {
            evaluacionRepository.syncEvaluaciones()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la sincronización: ${e.message}")
            Result.retry()  // Mecanismo de reintento para errores transitorios
        }
    }
}
