package com.agrojurado.sfmappv2.domain.usecase.usuario

import com.agrojurado.sfmappv2.domain.repository.UsuarioRepository
import javax.inject.Inject

class ListarUsuarioUseCase @Inject constructor(
    private val usuarioRepository: UsuarioRepository
){
    operator fun invoke(dato:String) = usuarioRepository.listar(dato)
}