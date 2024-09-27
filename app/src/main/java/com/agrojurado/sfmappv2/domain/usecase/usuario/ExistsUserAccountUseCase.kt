package com.agrojurado.sfmappv2.domain.usecase.usuario

import com.agrojurado.sfmappv2.domain.repository.UsuarioRepository
import javax.inject.Inject

class ExistsUserAccountUseCase @Inject constructor(
    private val usuarioRepository: UsuarioRepository
){
    suspend operator fun invoke() = usuarioRepository.existsAccount()
}
