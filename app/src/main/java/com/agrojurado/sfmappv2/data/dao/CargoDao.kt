package com.agrojurado.sfmappv2.data.dao

import androidx.room.*
import com.agrojurado.sfmappv2.data.entity.CargoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CargoDao {

    // Inserta un nuevo Cargo. Si ya existe, lo reemplaza.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCargo(cargo: CargoEntity): Long

    // Obtiene todos los Cargos como un flujo reactivo.
    @Query("SELECT * FROM cargo")
    fun getAllCargos(): Flow<List<CargoEntity>>

    // Obtiene un Cargo por su ID.
    @Query("SELECT * FROM cargo WHERE id = :id")
    suspend fun getCargoById(id: Int): CargoEntity?

    // Actualiza un Cargo existente.
    @Update
    suspend fun updateCargo(cargo: CargoEntity)

    // Elimina un Cargo específico.
    @Delete
    suspend fun deleteCargo(cargo: CargoEntity)

    // Elimina todos los Cargos de la base de datos.
    @Query("DELETE FROM cargo")
    suspend fun deleteAllCargos()

    // Verifica si existe algún Cargo (puedes agregar esto si es necesario).
    @Query("SELECT COUNT(*) FROM cargo")
    suspend fun countCargos(): Int
}
