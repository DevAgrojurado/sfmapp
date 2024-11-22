package com.agrojurado.sfmappv2.domain.usecase.evaluacion

import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import com.agrojurado.sfmappv2.domain.repository.EvaluacionPolinizacionRepository
import javax.inject.Inject

class InsertEvaluacionUseCase @Inject constructor(private val repository: EvaluacionPolinizacionRepository) {
    suspend operator fun invoke(evaluacion: EvaluacionPolinizacion): Long = repository.insertEvaluacion(evaluacion)
}