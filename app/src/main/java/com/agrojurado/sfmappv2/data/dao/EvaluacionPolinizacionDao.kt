package com.agrojurado.sfmappv2.data.dao

import androidx.room.*
import com.agrojurado.sfmappv2.data.entity.EvaluacionPolinizacionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EvaluacionPolinizacionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluacion(evaluacion: EvaluacionPolinizacionEntity): Long

    @Update
    suspend fun updateEvaluacion(evaluacion: EvaluacionPolinizacionEntity)

    @Delete
    suspend fun deleteEvaluacion(evaluacion: EvaluacionPolinizacionEntity)

    @Query("SELECT * FROM evaluacionpolinizacion")
    fun getEvaluaciones(): Flow<List<EvaluacionPolinizacionEntity>>

    @Query("SELECT * FROM evaluacionpolinizacion WHERE id = :id")
    suspend fun getEvaluacionById(id: Long): EvaluacionPolinizacionEntity?
}