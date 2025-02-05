package com.agrojurado.sfmappv2.presentation.ui.admin.usuarios

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class UsuariosViewModel @Inject constructor(
    private val usuarioRepository: UsuarioRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "UsuariosViewModel"
    }

    // Estado de la lista de usuarios
    val usuarios: LiveData<List<Usuario>> = usuarioRepository.getAllUsersUseCase().asLiveData()

    // Variables para manejar el estado de carga y errores
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

    init {
        observeNetworkState()
    }

    private fun observeNetworkState() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                viewModelScope.launch {
                    try {
                        _isLoading.value = true
                        syncUsuarios()
                    } catch (e: Exception) {
                        _error.value = "Error al sincronizar: ${e.message}"
                        Log.e(TAG, "Error durante la sincronización: ${e.message}", e)
                    } finally {
                        _isLoading.value = false
                    }
                }
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    fun deleteUsuario(usuario: Usuario) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            if (usuario.id == null) {
                _error.value = "ID de usuario no válido"
                Log.e(TAG, "El usuario no tiene un ID válido: $usuario")
                _isLoading.value = false
                return@launch
            }

            try {
                if (!_isOnline.value) {
                    _error.value = "No hay conexión a Internet. No se puede eliminar el usuario."
                    Log.e(TAG, "No hay conexión a Internet. No se puede eliminar el usuario.")
                    _isLoading.value = false
                    return@launch
                }

                val result = usuarioRepository.deleteUsuario(usuario)
                if (result > 0) {
                    Log.d(TAG, "Usuario eliminado con éxito: ${usuario.id}")
                    if (_isOnline.value) {
                        syncUsuarios()
                    }
                } else {
                    _error.value = "Error al eliminar usuario: no se completó la operación"
                    Log.e(TAG, "Eliminación no completada para el usuario con ID ${usuario.id}")
                }
            } catch (e: Exception) {
                _error.value = "Error al eliminar usuario: ${e.message}"
                Log.e(TAG, "Error al eliminar usuario con ID ${usuario.id}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateUsuario(usuario: Usuario) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                if (!_isOnline.value) {
                    _error.value = "No hay conexión a Internet. No se puede actualizar el usuario."
                    return@launch
                }

                val result = usuarioRepository.updateUsuario(usuario)
                if (result > 0) {
                    if (_isOnline.value) {
                        syncUsuarios()
                    }
                } else {
                    _error.value = "Error al actualizar usuario: no se completó la operación"
                }
            } catch (e: Exception) {
                _error.value = "Error al actualizar usuario: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun syncUsuarios() {
        try {
            usuarioRepository.syncUsuarios()  // Sincroniza los datos con el servidor
            Log.d(TAG, "Sincronización de usuarios completada con éxito")
        } catch (e: Exception) {
            _error.value = "Error en la sincronización: ${e.message}"
            Log.e(TAG, "Error durante la sincronización de usuarios", e)
        }
    }

    fun performFullSync() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = usuarioRepository.fullSync()  // Sincronización completa
                if (!success) {
                    _error.value = "Error en la sincronización completa"
                    Log.e(TAG, "La sincronización completa falló")
                } else {
                    Log.d(TAG, "Sincronización completa realizada con éxito")
                }
            } catch (e: Exception) {
                _error.value = "Error en la sincronización: ${e.message}"
                Log.e(TAG, "Error durante la sincronización completa", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
