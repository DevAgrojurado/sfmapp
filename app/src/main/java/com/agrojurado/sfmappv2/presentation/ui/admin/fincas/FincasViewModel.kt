package com.agrojurado.sfmappv2.presentation.ui.admin.fincas

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrojurado.sfmappv2.domain.model.Finca
import com.agrojurado.sfmappv2.domain.repository.FincaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FincasViewModel @Inject constructor(
    private val fincaRepository: FincaRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _fincas = MutableStateFlow<List<Finca>>(emptyList())
    val fincas: StateFlow<List<Finca>> = _fincas

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

    init {
        observeNetworkState()
        loadFincas()
    }

    private fun observeNetworkState() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                viewModelScope.launch {
                    try {
                        _isLoading.value = true
                        syncFincas()
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

    private fun loadFincas() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                fincaRepository.getAllFincas().collect { fincaList ->
                    _fincas.value = fincaList
                }
            } catch (e: Exception) {
                _error.value = "Error al cargar fincas: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun insertFinca(finca: Finca) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                fincaRepository.insertFinca(finca)

                if (_isOnline.value) {
                    syncFincas()
                }
            } catch (e: Exception) {
                _error.value = "Error al insertar finca: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateFinca(finca: Finca) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                fincaRepository.updateFinca(finca)

                if (_isOnline.value) {
                    syncFincas()
                }
            } catch (e: Exception) {
                _error.value = "Error al actualizar finca: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteFinca(finca: Finca) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                fincaRepository.deleteFinca(finca)

                if (_isOnline.value) {
                    syncFincas()
                }
            } catch (e: Exception) {
                _error.value = "Error al eliminar finca: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun syncFincas() {
        try {
            fincaRepository.syncFincas()
            loadFincas()
        } catch (e: Exception) {
            _error.value = "Error en la sincronización: ${e.message}"
        }
    }

    fun performFullSync() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val success = fincaRepository.fullSync()
                if (success) {
                    loadFincas()
                }
            } catch (e: Exception) {
                _error.value = "Error en la sincronización: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}