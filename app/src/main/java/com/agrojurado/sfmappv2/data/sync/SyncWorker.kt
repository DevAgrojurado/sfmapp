package com.agrojurado.sfmappv2.data.sync

import android.content.Context
import android.widget.ProgressBar
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.agrojurado.sfmappv2.data.remote.dto.common.utils.NetworkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SyncWorker @Inject constructor(
    @ApplicationContext private val context: Context,
    params: WorkerParameters,
    private val dataSyncManager: DataSyncManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val networkSpeed = NetworkManager.getNetworkSpeed(context)
        return try {
            if (networkSpeed == NetworkManager.NetworkSpeed.NONE) {
                NetworkManager.logError("SyncWorker", Exception("No network"), "Cannot sync due to no network")
                return Result.retry()
            }

            // Perform sync without progress bar for background work
            dataSyncManager.syncAllData(
                progressBar = ProgressBar(context).apply { visibility = ProgressBar.GONE },
                onSyncComplete = {}
            )
            Result.success()
        } catch (e: Exception) {
            NetworkManager.logError("SyncWorker", e, "Background sync failed")
            Result.retry()
        }
    }
}