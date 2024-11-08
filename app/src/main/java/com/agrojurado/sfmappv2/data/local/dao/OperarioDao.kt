package com.agrojurado.sfmappv2.data.local.dao

import androidx.room.*
import com.agrojurado.sfmappv2.data.local.entity.OperarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OperarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperario(operarioEntity: OperarioEntity): Long

    @Query("SELECT * FROM operario")
    fun getAllOperarios(): Flow<List<OperarioEntity>>

    @Query("SELECT * FROM operario WHERE id = :id")
    suspend fun getOperarioById(id: Int): OperarioEntity?

    @Update
    suspend fun updateOperario(operario: OperarioEntity)

    @Delete
    suspend fun deleteOperario(operario: OperarioEntity)

    @Query("DELETE FROM operario")
    suspend fun deleteAllOperarios()

    @Query("SELECT COUNT(*) FROM operario")
    suspend fun countOperario(): Int

    @Query("SELECT * FROM operario WHERE nombre LIKE '%' || :query || '%' OR codigo LIKE '%' || :query || '%'")
    fun searchOperarios(query: String): Flow<List<OperarioEntity>>
}
