package com.agrojurado.sfmappv2.data.local.dao

import androidx.room.*
import com.agrojurado.sfmappv2.data.local.entity.LoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLote(lote: LoteEntity): Long

    @Query("SELECT * FROM lote")
    fun getAllLotes(): Flow<List<LoteEntity>>

    @Query("SELECT * FROM lote WHERE id = :id LIMIT 1")
    suspend fun getLoteById(id: Int): LoteEntity?

    @Update
    suspend fun updateLote(lote: LoteEntity)

    @Delete
    suspend fun deleteLote(lote: LoteEntity)

    @Query("DELETE FROM lote")
    suspend fun deleteAllLotes()

    @Query("SELECT COUNT(*) FROM lote")
    suspend fun countLotes(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(lote: LoteEntity) : Long

    @Transaction
    suspend fun upsertLote(lote: LoteEntity) {
        val id = insertLote(lote)
        if (id == -1L) updateLote(lote)
    }

}

