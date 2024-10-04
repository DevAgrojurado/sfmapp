package com.agrojurado.sfmappv2.presentation.ui.login

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.domain.usecase.cargo.CrearCargoPredeterminadoUseCase
import com.agrojurado.sfmappv2.domain.usecase.usuario.CrearUsuarioPredeterminadoUseCase
import com.agrojurado.sfmappv2.domain.usecase.usuario.ExistsUserAccountUseCase
import com.agrojurado.sfmappv2.domain.usecase.usuario.GetUserUseCase
import com.agrojurado.sfmappv2.presentation.common.UiState
import com.agrojurado.sfmappv2.presentation.common.makeCall
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val getUserUseCase: GetUserUseCase,
    private val existsUserAccountUseCase: ExistsUserAccountUseCase,
    private val crearUsuarioPredeterminadoUseCase: CrearUsuarioPredeterminadoUseCase,
    private val crearCargoPredeterminado: CrearCargoPredeterminadoUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPreferences = context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE)

    init {
        viewModelScope.launch {
            crearCargoPredeterminado() // Crear el usuario predeterminado
        }
    }

    private val _uiStateLogin = MutableLiveData<UiState<Usuario?>?>()
    val uiStateLogin: LiveData<UiState<Usuario?>?> = _uiStateLogin

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

        makeCall { getUserUseCase(email, clave) }.let { result ->
            _uiStateLogin.value = result
            if (result is UiState.Success && result.data != null) {
                guardarSesionUsuario(result.data)
            }
        }
    }

    fun existeCuenta() = viewModelScope.launch {
        _uiStateExisteCuenta.value = UiState.Loading

        makeCall { existsUserAccountUseCase() }.let {
            _uiStateExisteCuenta.value = it
        }
    }

    private fun guardarSesionUsuario(usuario: Usuario) {
        sharedPreferences.edit().apply {
            putBoolean("sesion_iniciada", true)
            putString("email_usuario", usuario.email)
            putString("nombre_usuario", usuario.nombre)
            putString("codigo_usuario", usuario.codigo)
            // Añade otros detalles del usuario según sea necesario
            apply()
        }
    }

    fun isSessionStarted(): Boolean {
        return sharedPreferences.getBoolean("sesion_iniciada", false)
    }

    fun cerrarSesion() {
        sharedPreferences.edit().clear().apply()
    }

    fun obtenerEmailUsuarioConSesion(): String? {
        return sharedPreferences.getString("email_usuario", null)
    }
}