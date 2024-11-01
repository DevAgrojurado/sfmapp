package com.agrojurado.sfmappv2.presentation.ui.admin.cargos

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrojurado.sfmappv2.domain.model.Cargo
import com.agrojurado.sfmappv2.domain.repository.CargoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CargosViewModel @Inject constructor(
    private val repository: CargoRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _cargos = MutableStateFlow<List<Cargo>>(emptyList())
    val cargos: StateFlow<List<Cargo>> = _cargos

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

    init {
        observeNetworkState()
        loadCargos()
    }

    private fun observeNetworkState() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                viewModelScope.launch {
                    try {
                        _isLoading.value = true
                        syncCargos()
                    } catch (e: Exception) {
                        _error.value = "Error al sincronizar: ${e.message}"
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

    private fun loadCargos() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.getAllCargos().collect { cargoList ->
                    _cargos.value = cargoList
                }
            } catch (e: Exception) {
                _error.value = "Error al cargar cargos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun insertCargo(cargo: Cargo) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                repository.insertCargo(cargo)

                if (_isOnline.value) {
                    syncCargos()
                }
            } catch (e: Exception) {
                _error.value = "Error al insertar cargo: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateCargo(cargo: Cargo) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                repository.updateCargo(cargo)

                if (_isOnline.value) {
                    syncCargos()
                }
            } catch (e: Exception) {
                _error.value = "Error al actualizar cargo: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteCargo(cargo: Cargo) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                repository.deleteCargo(cargo)

                if (_isOnline.value) {
                    syncCargos()
                }
            } catch (e: Exception) {
                _error.value = "Error al eliminar cargo: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun syncCargos() {
        try {
            repository.syncCargos()
            loadCargos()
        } catch (e: Exception) {
            _error.value = "Error en la sincronización: ${e.message}"
        }
    }

    fun performFullSync() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val success = repository.fullSync()
                if (success) {
                    loadCargos() // Recargar los cargos después de la sincronización
                }
            } catch (e: Exception) {
                _error.value = "Error en la sincronización: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}