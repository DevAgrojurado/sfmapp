package com.agrojurado.sfmappv2.domain.usecase.operario

import com.agrojurado.sfmappv2.domain.repository.OperarioRepository
import javax.inject.Inject

class DeleteAllOperariosUseCase @Inject constructor(private val repository: OperarioRepository) {
    suspend operator fun invoke() = repository.deleteAllOperarios()
}