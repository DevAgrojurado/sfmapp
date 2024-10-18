package com.agrojurado.sfmappv2.data.dao

import androidx.room.*
import com.agrojurado.sfmappv2.data.entity.FincaEntity
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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(finca: FincaEntity) : Long

    @Query("SELECT * FROM cargo WHERE id = 0 LIMIT 1")
    suspend fun getFincaPredeterminado(): FincaEntity?

    @Transaction
    suspend fun indertFincaIfNotExists(finca: FincaEntity) {
        val existingFinca = getFincaPredeterminado()
        if (existingFinca == null) {
            insertFinca(finca)
        }
    }
}

