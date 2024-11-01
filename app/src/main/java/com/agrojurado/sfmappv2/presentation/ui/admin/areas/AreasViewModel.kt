package com.agrojurado.sfmappv2.presentation.ui.admin.areas

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrojurado.sfmappv2.domain.model.Area
import com.agrojurado.sfmappv2.domain.repository.AreaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AreasViewModel @Inject constructor(
    private val areaRepository: AreaRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _areas = MutableStateFlow<List<Area>>(emptyList())
    val areas: StateFlow<List<Area>> = _areas

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

    init {
        observeNetworkState()
        loadAreas()
    }

    private fun observeNetworkState() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                viewModelScope.launch {
                    try {
                        _isLoading.value = true
                        syncAreas()
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

    private fun loadAreas() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                areaRepository.getAllAreas().collect { areaList ->
                    _areas.value = areaList
                }
            } catch (e: Exception) {
                _error.value = "Error al cargar áreas: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun insertArea(area: Area) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                areaRepository.insertArea(area)

                if (_isOnline.value) {
                    syncAreas()
                }
            } catch (e: Exception) {
                _error.value = "Error al insertar área: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateArea(area: Area) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                areaRepository.updateArea(area)

                if (_isOnline.value) {
                    syncAreas()
                }
            } catch (e: Exception) {
                _error.value = "Error al actualizar área: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteArea(area: Area) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                areaRepository.deleteArea(area)

                if (_isOnline.value) {
                    syncAreas()
                }
            } catch (e: Exception) {
                _error.value = "Error al eliminar área: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun syncAreas() {
        try {
            areaRepository.syncAreas()
            loadAreas()
        } catch (e: Exception) {
            _error.value = "Error en la sincronización: ${e.message}"
        }
    }

    fun performFullSync() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val success = areaRepository.fullSync()
                if (success) {
                    loadAreas() // Recargar las áreas después de la sincronización
                }
            } catch (e: Exception) {
                _error.value = "Error en la sincronización: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}