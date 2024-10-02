package com.agrojurado.sfmappv2.presentation.ui.admin.usuarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UsuariosViewModel @Inject constructor(
    private val usuarioRepository: UsuarioRepository) : ViewModel() {
    val usuarios = usuarioRepository.getAllUsersUseCase().asLiveData()

    suspend fun deleteUsuario(usuario: Usuario) {
        usuarioRepository.deleteUsuario(usuario)
    }

}
