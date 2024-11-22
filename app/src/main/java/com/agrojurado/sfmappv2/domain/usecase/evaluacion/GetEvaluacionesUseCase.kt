package com.agrojurado.sfmappv2.domain.usecase.evaluacion

import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import com.agrojurado.sfmappv2.domain.repository.EvaluacionPolinizacionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEvaluacionesUseCase @Inject constructor(private val repository: EvaluacionPolinizacionRepository) {
    operator fun invoke(): Flow<List<EvaluacionPolinizacion>> = repository.getEvaluaciones()
}