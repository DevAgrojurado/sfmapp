package com.agrojurado.sfmappv2.domain.usecase.evaluacion

import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import com.agrojurado.sfmappv2.domain.repository.EvaluacionPolinizacionRepository
import com.agrojurado.sfmappv2.domain.repository.FincaRepository
import javax.inject.Inject

class DeleteEvaluacionUseCase @Inject constructor(private val repository: EvaluacionPolinizacionRepository) {
    suspend operator fun invoke(evaluacion: EvaluacionPolinizacion) = repository.deleteEvaluacion(evaluacion)
}