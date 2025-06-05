package com.agrojurado.sfmappv2.domain.repository

import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import kotlinx.coroutines.flow.Flow

interface EvaluacionPolinizacionRepository {

    suspend fun insertEvaluacion(evaluacion: EvaluacionPolinizacion): Long
    suspend fun updateEvaluacion(evaluacion: EvaluacionPolinizacion)
    suspend fun deleteEvaluacion(evaluacion: EvaluacionPolinizacion)
    fun getEvaluaciones(): Flow<List<EvaluacionPolinizacion>>
    suspend fun getEvaluacionById(id: Int): EvaluacionPolinizacion?
    suspend fun getLastEvaluacion(): EvaluacionPolinizacion?
    suspend fun checkPalmExists(semana: Int, lote: Int, palma: Int, idPolinizador: Int, seccion: Int, evaluacionGeneralId: Int): Boolean
    //suspend fun syncEvaluaciones()
    //suspend fun syncEvaluacionesForGeneral(evaluaciones: List<EvaluacionPolinizacion>, serverGeneralId: Int)
    suspend fun deleteEvaluacionesByEvaluacionGeneralId(evaluacionGeneralId: Int)
    suspend fun associateWithGeneralEvaluation(evaluacionId: Int, evaluacionGeneralId: Int)
    fun getEvaluacionesByEvaluacionGeneralId(evaluacionGeneralId: Int): Flow<List<EvaluacionPolinizacion>>
    //suspend fun fetchEvaluacionesFromServer()
    suspend fun getUnsyncedEvaluationsCount(): Int

}