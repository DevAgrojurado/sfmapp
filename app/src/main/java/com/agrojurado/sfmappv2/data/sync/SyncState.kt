package com.agrojurado.sfmappv2.data.sync

sealed class SyncState {
    object Idle : SyncState()
    data class Syncing(
        val totalItems: Int = 0,
        val currentItem: Int = 0,
        val currentOperation: String = "",
        val progress: Float = 0f
    ) : SyncState()
    data class Error(val message: String, val throwable: Throwable? = null) : SyncState()
    data class Success(val message: String) : SyncState()
}