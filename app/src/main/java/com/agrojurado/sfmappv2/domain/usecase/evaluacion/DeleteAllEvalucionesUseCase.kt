package com.agrojurado.sfmappv2.domain.usecase.evaluacion

import com.agrojurado.sfmappv2.domain.repository.EvaluacionPolinizacionRepository
import javax.inject.Inject

class DeleteAllEvalucionesUseCase @Inject constructor(private val repository: EvaluacionPolinizacionRepository) {

}