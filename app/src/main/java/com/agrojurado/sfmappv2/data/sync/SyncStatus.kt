package com.agrojurado.sfmappv2.data.sync

sealed class SyncStatus {
    object Syncing : SyncStatus()
    object Completed : SyncStatus()
    data class Pending(val unsyncedCount: Int) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}