package com.agrojurado.sfmappv2.domain.usecase.usuario

import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.domain.repository.UsuarioRepository
import javax.inject.Inject

class GrabarUsuarioUseCase @Inject constructor(
    private val usuarioRepository: UsuarioRepository
){
    suspend operator fun invoke(usuario: Usuario): Int{
        return usuarioRepository.grabar(usuario)
    }

}