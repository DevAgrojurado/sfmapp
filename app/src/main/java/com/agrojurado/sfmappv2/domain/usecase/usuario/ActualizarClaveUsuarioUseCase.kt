package com.agrojurado.sfmappv2.domain.usecase.usuario

import com.agrojurado.sfmappv2.domain.repository.UsuarioRepository
import javax.inject.Inject

class ActualizarClaveUsuarioUseCase @Inject constructor(
    private val usuarioRepository: UsuarioRepository
){
    suspend operator fun invoke(id: Int, clave: String): Int{
        return usuarioRepository.actualizarClave(id, clave)
    }

}