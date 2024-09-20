package com.agrojurado.sfmappv2.domain.usecase.operario

import com.agrojurado.sfmappv2.domain.model.Operario
import com.agrojurado.sfmappv2.domain.repository.OperarioRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllOperariosUseCase @Inject constructor(private val repository: OperarioRepository) {
    operator fun invoke(): Flow<List<Operario>> = repository.getAllOperarios()
}