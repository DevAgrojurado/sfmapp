package com.agrojurado.sfmappv2.domain.usecase.operario

import com.agrojurado.sfmappv2.domain.model.Operario
import com.agrojurado.sfmappv2.domain.repository.OperarioRepository
import javax.inject.Inject

class InsertOperarioUseCase @Inject constructor(private val repository: OperarioRepository) {
    suspend operator fun invoke(operario: Operario): Long = repository.insertOperario(operario)

}