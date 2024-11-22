package com.agrojurado.sfmappv2.domain.repository

import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import kotlinx.coroutines.flow.Flow

interface EvaluacionPolinizacionRepository {

    suspend fun insertEvaluacion(evaluacion: EvaluacionPolinizacion): Long

    suspend fun updateEvaluacion(evaluacion: EvaluacionPolinizacion)

    suspend fun deleteEvaluacion(evaluacion: EvaluacionPolinizacion)

    suspend fun deleteAllEvaluaciones()

    fun getEvaluaciones(): Flow<List<EvaluacionPolinizacion>>

    suspend fun getEvaluacionById(id: Int): EvaluacionPolinizacion?

    suspend fun getLastEvaluacion(): EvaluacionPolinizacion?

    suspend fun checkPalmExists(semana: Int, lote: Int, palma: Int, idPolinizador: Int): Boolean

    suspend fun fullSync(): Boolean

    suspend fun syncEvaluaciones()
}