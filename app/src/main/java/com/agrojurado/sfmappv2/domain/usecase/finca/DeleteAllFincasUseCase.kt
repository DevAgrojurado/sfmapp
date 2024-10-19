package com.agrojurado.sfmappv2.domain.usecase.finca

import com.agrojurado.sfmappv2.domain.repository.FincaRepository
import javax.inject.Inject

class DeleteAllFincasUseCase @Inject constructor(private val repository: FincaRepository) {
    suspend operator fun invoke() = repository.deleteAllFincas()
}