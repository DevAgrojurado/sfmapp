package com.agrojurado.sfmappv2.presentation.ui.crearcuenta

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.domain.usecase.usuario.GrabarCuentaUsuarioUseCase
import com.agrojurado.sfmappv2.presentation.common.UiState
import com.agrojurado.sfmappv2.presentation.common.makeCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CrearCuentaViewModel @Inject constructor(
    private val useCase: GrabarCuentaUsuarioUseCase
) : ViewModel() {

    private val _uiStateGrabar = MutableLiveData<UiState<Usuario?>?>()
    val uiStateGrabar: LiveData<UiState<Usuario?>?> = _uiStateGrabar

    fun resetUiStateGrabar() {
        _uiStateGrabar.value = null
    }

    fun grabarCuenta(usuario: Usuario) = viewModelScope.launch {
        _uiStateGrabar.value = UiState.Loading

        makeCall { useCase(usuario) }.let {
            _uiStateGrabar.value = it
        }
    }

}