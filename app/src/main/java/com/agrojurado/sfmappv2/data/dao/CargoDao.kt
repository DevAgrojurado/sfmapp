package com.agrojurado.sfmappv2.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.agrojurado.sfmappv2.data.entity.CargoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CargoDao {

    @Insert
    suspend fun insertar(cargo: CargoEntity): Long

    @Insert
    suspend fun insertarCargo(cargos: List<CargoEntity>)

    @Update
    suspend fun actualizar(cargo: CargoEntity): Int

    @Delete
    suspend fun eliminar(cargo: CargoEntity): Int

    @Query("SELECT * FROM cargo WHERE descripcion LIKE '%' || :descripcion || '%'")
    fun listarPorDescripcion(descripcion: String): Flow<List<CargoEntity>>

    @Query("SELECT * FROM cargo WHERE id=:id")
    suspend fun obtenerCargoPorId(id: Int): CargoEntity?

    @Transaction
    suspend fun grabarCargo(cargo: CargoEntity): CargoEntity? {
        return obtenerCargoPorId(insertar(cargo).toInt())
    }
}
