package com.agrojurado.sfmappv2.data.local.dao

import androidx.room.*
import com.agrojurado.sfmappv2.data.local.entity.CargoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CargoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCargo(cargo: CargoEntity): Long

    @Query("SELECT * FROM cargo")
    fun getAllCargos(): Flow<List<CargoEntity>>

    @Query("SELECT * FROM cargo WHERE id = :id LIMIT 1")
    suspend fun getCargoById(id: Int): CargoEntity?

    @Update
    suspend fun updateCargo(cargo: CargoEntity)

    @Delete
    suspend fun deleteCargo(cargo: CargoEntity)

    @Query("DELETE FROM cargo")
    suspend fun deleteAllCargos()

    @Query("SELECT COUNT(*) FROM cargo")
    suspend fun countCargos(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(cargo: CargoEntity) : Long

    @Query("SELECT * FROM cargo WHERE id = 0 LIMIT 1")
    suspend fun getCargoPredeterminado(): CargoEntity?

    @Transaction
    suspend fun upsertCargo(cargo: CargoEntity) {
        val id = insertCargo(cargo)
        if (id == -1L) updateCargo(cargo)
    }

    @Transaction
    suspend fun transaction(block: suspend () -> Unit) = block()
}

