package com.agrojurado.sfmappv2.data.local.dao

import androidx.room.*
import com.agrojurado.sfmappv2.data.local.entity.EvaluacionPolinizacionEntity
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

    @Query("SELECT * FROM evaluacionpolinizacion WHERE id = :id LIMIT 1")
    suspend fun getEvaluacionById(id: Int): EvaluacionPolinizacionEntity?

    @Query("SELECT * FROM evaluacionpolinizacion WHERE serverId = :serverId LIMIT 1")
    suspend fun getEvaluacionByServerId(serverId: Int): EvaluacionPolinizacionEntity?

    @Query("SELECT * FROM evaluacionpolinizacion ORDER BY id DESC LIMIT 1")
    suspend fun getLastEvaluacion(): EvaluacionPolinizacionEntity?

    @Query("SELECT COUNT(*) FROM evaluacionpolinizacion WHERE semana = :semana AND idlote = :idlote AND palma = :palma AND idPolinizador = :idPolinizador AND seccion = :seccion AND evaluacionGeneralId = :evaluacionGeneralId")
    suspend fun checkPalmExists(semana: Int, idlote: Int, palma: Int, idPolinizador: Int, seccion: Int, evaluacionGeneralId: Int): Int

    @Query("DELETE FROM evaluacionpolinizacion WHERE id = :id")
    suspend fun deleteEvaluacionById(id: Int)

    @Query("DELETE FROM evaluacionpolinizacion WHERE evaluacionGeneralId = :evaluacionGeneralId")
    suspend fun deleteByEvaluacionGeneralId(evaluacionGeneralId: Int)

    @Query("UPDATE evaluacionpolinizacion SET evaluacionGeneralId = :evaluacionGeneralId WHERE id = :evaluacionId")
    suspend fun updateEvaluacionGeneralId(evaluacionId: Int, evaluacionGeneralId: Int)

    @Query("SELECT * FROM evaluacionpolinizacion WHERE evaluacionGeneralId = :evaluacionGeneralId")
    fun getEvaluacionesByEvaluacionGeneralId(evaluacionGeneralId: Int): Flow<List<EvaluacionPolinizacionEntity>>

    @Query("SELECT COUNT(*) FROM evaluacionpolinizacion WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getUnsyncedEvaluationsCount(): Int

    @Transaction
    suspend fun transaction(block: suspend () -> Unit) = block()

    @Transaction
    suspend fun insertOrUpdate(evaluacion: EvaluacionPolinizacionEntity) {
        val existing = getEvaluacionById(evaluacion.id)
        if (existing != null) {
            updateEvaluacion(evaluacion)
        } else {
            insertEvaluacion(evaluacion)
        }
    }
}