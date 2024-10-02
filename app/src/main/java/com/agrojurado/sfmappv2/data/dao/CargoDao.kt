package com.agrojurado.sfmappv2.data.dao

import androidx.room.*
import com.agrojurado.sfmappv2.data.entity.CargoEntity
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
    suspend fun insertCargoIfNotExists(cargo: CargoEntity) {
        val existingCargo = getCargoPredeterminado()
        if (existingCargo == null) {
            insertCargo(cargo)
        }
    }
}

