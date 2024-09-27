package com.agrojurado.sfmappv2.presentation.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.domain.usecase.usuario.CrearUsuarioPredeterminadoUseCase
import com.agrojurado.sfmappv2.domain.usecase.usuario.ExistsUserAccountUseCase
import com.agrojurado.sfmappv2.domain.usecase.usuario.GetUserUseCase
import com.agrojurado.sfmappv2.presentation.common.UiState
import com.agrojurado.sfmappv2.presentation.common.makeCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val GetUserUseCase: GetUserUseCase,
    private val existsUserAccountUseCase: ExistsUserAccountUseCase,
    private val crearUsuarioPredeterminadoUseCase: CrearUsuarioPredeterminadoUseCase
): ViewModel() {

    init {
        viewModelScope.launch {
            crearUsuarioPredeterminadoUseCase() // Crear el usuario predeterminado
        }
    }

    private val _uiStateLogin = MutableLiveData<UiState<Usuario?>?>()
    val UiStateLogin: LiveData<UiState<Usuario?>?> = _uiStateLogin

    private val _uiStateExisteCuenta = MutableLiveData<UiState<Int>?>()
    val uiStateExisteCuenta: LiveData<UiState<Int>?> = _uiStateExisteCuenta

    fun resetUiStateLogin() {
        _uiStateLogin.value = null
    }

    fun resetUiStateExisteCuenta() {
        _uiStateExisteCuenta.value = null
    }

    fun login(email: String, clave: String) = viewModelScope.launch {
        _uiStateLogin.value = UiState.Loading

        makeCall { GetUserUseCase(email, clave) }.let {
            _uiStateLogin.value = it
        }
    }

    fun existeCuenta() = viewModelScope.launch {
        _uiStateExisteCuenta.value = UiState.Loading

        makeCall { existsUserAccountUseCase() }.let {
            _uiStateExisteCuenta.value = it
        }
    }

}