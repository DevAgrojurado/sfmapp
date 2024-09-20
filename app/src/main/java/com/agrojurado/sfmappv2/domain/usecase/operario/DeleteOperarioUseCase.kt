package com.agrojurado.sfmappv2.domain.usecase.operario

import com.agrojurado.sfmappv2.domain.model.Operario
import com.agrojurado.sfmappv2.domain.repository.OperarioRepository
import javax.inject.Inject

class DeleteOperarioUseCase @Inject constructor(private val repository: OperarioRepository) {
    suspend operator fun invoke(operario: Operario) = repository.deleteOperario(operario)
}