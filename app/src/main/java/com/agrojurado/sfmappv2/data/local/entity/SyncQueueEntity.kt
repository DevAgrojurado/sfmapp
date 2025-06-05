package com.agrojurado.sfmappv2.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue", indices = [Index(value = ["entityType", "entityId"], unique = true)])
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,
    val entityId: Int,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)