package com.agrojurado.sfmappv2.domain.usecase.usuario

import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.domain.repository.UsuarioRepository
import javax.inject.Inject

class DeleteUserUseCase @Inject constructor(
    private val usuarioRepository: UsuarioRepository
) {
    suspend operator fun invoke(usuario: Usuario): Int {
        return usuarioRepository.delete(usuario)
    }

}