package com.agrojurado.sfmappv2.domain.usecase.finca

import com.agrojurado.sfmappv2.domain.model.Finca
import com.agrojurado.sfmappv2.domain.repository.FincaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllFincasUseCase @Inject constructor(private val repository: FincaRepository) {
    operator fun invoke(): Flow<List<Finca>> = repository.getAllFincas()
}