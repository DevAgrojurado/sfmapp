package com.agrojurado.sfmappv2.presentation.ui.login

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrojurado.sfmappv2.data.mapper.UsuarioMapper
import com.agrojurado.sfmappv2.data.remote.dto.common.utils.Utils
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.domain.repository.UsuarioRepository
import com.agrojurado.sfmappv2.domain.usecase.cargo.CrearCargoPredeterminadoUseCase
import com.agrojurado.sfmappv2.domain.usecase.usuario.CrearUsuarioPredeterminadoUseCase
import com.agrojurado.sfmappv2.domain.usecase.usuario.ExistsUserAccountUseCase
import com.agrojurado.sfmappv2.domain.usecase.usuario.GetUserUseCase
import com.agrojurado.sfmappv2.presentation.common.UiState
import com.agrojurado.sfmappv2.presentation.common.UiState.Success
import com.agrojurado.sfmappv2.presentation.common.makeCall
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import pe.pcs.libpcs.UtilsSecurity
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val getUserUseCase: GetUserUseCase,
    private val existsUserAccountUseCase: ExistsUserAccountUseCase,
    private val crearUsuarioPredeterminadoUseCase: CrearUsuarioPredeterminadoUseCase,
    private val usuarioRepository: UsuarioRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPreferences = context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE)

    init {
        viewModelScope.launch {
            crearUsuarioPredeterminadoUseCase // Crear el usuario predeterminado
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
        Log.d("LoginViewModel", "Iniciando login con email: $email")

        try {
            if (!Utils.isNetworkAvailable(context)) {
                _uiStateLogin.value = UiState.Error("No hay conexión a internet. Por favor, verifica tu conexión.")
                return@launch
            }

            val loginResponse = usuarioRepository.login(email, clave)

            if (loginResponse != null && loginResponse.success) {
                if (loginResponse.usuario != null) {
                    // Aquí ya tienes el tipo correcto UsuarioResponse
                    val usuario = UsuarioMapper.fromResponse(loginResponse.usuario)
                    Log.d("LoginViewModel", "Login exitoso en servidor")
                    guardarSesionUsuario(usuario)
                    _uiStateLogin.value = UiState.Success(usuario)
                } else {
                    Log.d("LoginViewModel", "Usuario no encontrado en la respuesta del servidor")
                    _uiStateLogin.value = UiState.Error("Usuario no encontrado o datos inválidos.")
                }
            } else {
                Log.d("LoginViewModel", "Credenciales inválidas o login fallido")
                _uiStateLogin.value = UiState.Error("Credenciales inválidas. Por favor, verifique su email y contraseña.")
            }
        } catch (e: Exception) {
            Log.e("LoginViewModel", "Error durante el login: ${e.message}", e)
            _uiStateLogin.value = UiState.Error("Error durante el inicio de sesión: ${e.message}")
        }
    }
    fun existeCuenta() = viewModelScope.launch {
        _uiStateExisteCuenta.value = UiState.Loading

        makeCall { existsUserAccountUseCase() }.let {
            _uiStateExisteCuenta.value = it
        }
    }

    private fun guardarSesionUsuario(usuario: Usuario) {
        Log.d("LoginViewModel", "Guardando sesión para usuario: ${usuario.email}")
        sharedPreferences.edit().apply {
            putBoolean("sesion_iniciada", true)
            putString("email_usuario", usuario.email)
            putString("nombre_usuario", usuario.nombre)
            putString("codigo_usuario", usuario.codigo)
            apply()
        }
        Log.d("LoginViewModel", "Sesión guardada correctamente")
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