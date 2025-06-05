package com.agrojurado.sfmappv2.domain.model

data class SyncQueue(
    val id: Int = 0,
    val entityType: String,
    val entityId: Int,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
