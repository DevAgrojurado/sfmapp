package com.agrojurado.sfmappv2.data.local.dao

import androidx.room.*
import com.agrojurado.sfmappv2.data.local.entity.EvaluacionPolinizacionEntity
import com.agrojurado.sfmappv2.data.local.entity.OperarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EvaluacionPolinizacionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluacion(evaluacion: EvaluacionPolinizacionEntity): Long

    @Insert
    suspend fun insertEvaluaciones(evaluaciones: List<EvaluacionPolinizacionEntity>)

    @Update
    suspend fun updateEvaluacion(evaluacion: EvaluacionPolinizacionEntity)

    @Delete
    suspend fun deleteEvaluacion(evaluacion: EvaluacionPolinizacionEntity)

    //@Query("DELETE FROM evaluacionpolinizacion")
    //suspend fun deleteAllEvaluaciones()

    @Query("SELECT * FROM evaluacionpolinizacion")
    fun getEvaluaciones(): Flow<List<EvaluacionPolinizacionEntity>>

    @Query("SELECT * FROM evaluacionpolinizacion WHERE id = :id LIMIT 1")
    suspend fun getEvaluacionById(id: Int): EvaluacionPolinizacionEntity?

    @Query("SELECT * FROM evaluacionpolinizacion ORDER BY id DESC LIMIT 1")
    suspend fun getLastEvaluacion(): EvaluacionPolinizacionEntity?

    @Query("SELECT COUNT(*) FROM evaluacionpolinizacion WHERE semana = :semana AND idlote = :idlote AND palma = :palma AND idPolinizador = :idPolinizador")
    suspend fun checkPalmExists(semana: Int, idlote: Int, palma: Int, idPolinizador: Int): Int

    @Query("DELETE FROM evaluacionpolinizacion WHERE id = :id")
    suspend fun deleteEvaluacionById(id: Int)

    @Transaction
    suspend fun transaction(block: suspend () -> Unit) = block()

    @Transaction
    suspend fun insertOrUpdate(evaluacion: EvaluacionPolinizacionEntity) {
        val existing = getEvaluacionById(evaluacion.id!!)
        if (existing != null) {
            updateEvaluacion(evaluacion)
        } else {
            insertEvaluacion(evaluacion)
        }
    }
}