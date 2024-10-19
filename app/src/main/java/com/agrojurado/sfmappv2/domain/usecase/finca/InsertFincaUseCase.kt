package com.agrojurado.sfmappv2.domain.usecase.finca

import com.agrojurado.sfmappv2.domain.model.Finca
import com.agrojurado.sfmappv2.domain.repository.FincaRepository
import javax.inject.Inject

class InsertFincaUseCase @Inject constructor(private val repository: FincaRepository) {
    suspend operator fun invoke(finca: Finca): Long = repository.insertFinca(finca)
}