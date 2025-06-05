package com.agrojurado.sfmappv2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.agrojurado.sfmappv2.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) // Ignora si ya existe
    suspend fun enqueue(item: SyncQueueEntity): Long

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    fun getQueue(): Flow<List<SyncQueueEntity>>

    @Query("DELETE FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun dequeue(entityType: String, entityId: Int)

    @Update
    suspend fun update(item: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId LIMIT 1")
    suspend fun getQueueItem(entityType: String, entityId: Int): SyncQueueEntity?

    @Transaction
    suspend fun enqueueIfNotExists(item: SyncQueueEntity): Long {
        val existing = getQueueItem(item.entityType, item.entityId)
        return if (existing == null) {
            enqueue(item)
        } else {
            existing.id
        }
    }

    @Transaction
    suspend fun transaction(block: suspend () -> Unit) = block()
}