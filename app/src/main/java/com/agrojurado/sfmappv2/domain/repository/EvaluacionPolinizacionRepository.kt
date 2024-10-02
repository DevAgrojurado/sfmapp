package com.agrojurado.sfmappv2.domain.repository

import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import kotlinx.coroutines.flow.Flow

interface EvaluacionPolinizacionRepository {

    suspend fun insertEvaluacion(evaluacion: EvaluacionPolinizacion): Long

    suspend fun updateEvaluacion(evaluacion: EvaluacionPolinizacion)

    suspend fun deleteEvaluacion(evaluacion: EvaluacionPolinizacion)

    fun getEvaluaciones(): Flow<List<EvaluacionPolinizacion>>

    suspend fun getEvaluacionById(id: Long): EvaluacionPolinizacion?
}