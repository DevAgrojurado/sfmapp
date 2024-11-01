package com.agrojurado.sfmappv2.data.local.dao

import androidx.room.*
import com.agrojurado.sfmappv2.data.local.entity.OperarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OperarioDao {

    // Inserta un nuevo operario. Si ya existe, lo reemplaza.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperario(operarioEntity: OperarioEntity): Long

    // Obtiene todos los operario como un flujo reactivo.
    @Query("SELECT * FROM operario")
    fun getAllOperarios(): Flow<List<OperarioEntity>>

    // Obtiene un Operario por su ID.
    @Query("SELECT * FROM operario WHERE id = :id")
    suspend fun getOperarioById(id: Int): OperarioEntity?

    // Actualiza un Operario existente.
    @Update
    suspend fun updateOperario(operario: OperarioEntity)

    // Elimina un Operario específico.
    @Delete
    suspend fun deleteOperario(operario: OperarioEntity)

    // Elimina todos los operarios de la base de datos.
    @Query("DELETE FROM operario")
    suspend fun deleteAllOperarios()

    // Verifica si existe algún Cargo
    @Query("SELECT COUNT(*) FROM operario")
    suspend fun countOperario(): Int

    @Query("SELECT * FROM operario WHERE nombre LIKE '%' || :query || '%' OR codigo LIKE '%' || :query || '%'")
    fun searchOperarios(query: String): Flow<List<OperarioEntity>>
}
