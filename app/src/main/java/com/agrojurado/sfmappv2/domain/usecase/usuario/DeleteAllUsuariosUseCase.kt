package com.agrojurado.sfmappv2.domain.usecase.usuario

import com.agrojurado.sfmappv2.domain.repository.UsuarioRepository
import javax.inject.Inject

class DeleteAllUsuariosUseCase @Inject constructor(private val repository: UsuarioRepository) {
    suspend operator fun invoke() = repository.deleteAllUsuarios()
}