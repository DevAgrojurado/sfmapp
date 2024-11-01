package com.agrojurado.sfmappv2.data.local.dao

import androidx.room.*
import com.agrojurado.sfmappv2.data.local.entity.AreaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AreaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArea(area: AreaEntity): Long

    @Query("SELECT * FROM area")
    fun getAllAreas(): Flow<List<AreaEntity>>

    @Query("SELECT * FROM area WHERE id = :id")
    suspend fun getAreaById(id: Int): AreaEntity?

    @Update
    suspend fun updateArea(area: AreaEntity)

    @Delete
    suspend fun deleteArea(area: AreaEntity)

    @Query("DELETE FROM area")
    suspend fun deleteAllAreas()

    @Query("SELECT COUNT(*) FROM area")
    suspend fun countAreas(): Int

    @Transaction
    suspend fun upsertArea(area: AreaEntity) {
        val id = insertArea(area)
        if (id == -1L) updateArea(area)
    }
}
