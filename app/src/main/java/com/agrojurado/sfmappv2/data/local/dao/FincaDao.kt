package com.agrojurado.sfmappv2.data.local.dao

import androidx.room.*
import com.agrojurado.sfmappv2.data.local.entity.FincaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FincaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinca(finca: FincaEntity): Long

    @Query("SELECT * FROM finca")
    fun getAllFincas(): Flow<List<FincaEntity>>

    @Query("SELECT * FROM finca WHERE id = :id LIMIT 1")
    suspend fun getFincaById(id: Int): FincaEntity?

    @Update
    suspend fun updateFinca(finca: FincaEntity)

    @Delete
    suspend fun deleteFinca(finca: FincaEntity)

    @Query("DELETE FROM finca")
    suspend fun deleteAllFincas()

    @Query("SELECT COUNT(*) FROM finca")
    suspend fun CountFincas(): Int

    @Transaction
    suspend fun upsertFinca(finca: FincaEntity) {
        val id = insertFinca(finca)
        if (id == -1L) updateFinca(finca)
    }

    @Transaction
    suspend fun transaction(block: suspend () -> Unit) = block()

}

