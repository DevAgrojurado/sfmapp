package com.agrojurado.sfmappv2.presentation.ui.admin.operarios

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrojurado.sfmappv2.domain.model.Area
import com.agrojurado.sfmappv2.domain.model.Cargo
import com.agrojurado.sfmappv2.domain.model.Finca
import com.agrojurado.sfmappv2.domain.model.Operario
import com.agrojurado.sfmappv2.domain.repository.AreaRepository
import com.agrojurado.sfmappv2.domain.repository.CargoRepository
import com.agrojurado.sfmappv2.domain.repository.FincaRepository
import com.agrojurado.sfmappv2.domain.repository.OperarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OperariosViewModel @Inject constructor(
    private val operarioRepository: OperarioRepository,
    private val cargoRepository: CargoRepository,
    private val areaRepository: AreaRepository,
    private val fincaRepository: FincaRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _operarios = MutableStateFlow<List<Operario>>(emptyList())
    val operarios: StateFlow<List<Operario>> = _operarios

    private val _cargos = MutableStateFlow<List<Cargo>>(emptyList())
    val cargos: StateFlow<List<Cargo>> = _cargos

    private val _areas = MutableStateFlow<List<Area>>(emptyList())
    val areas: StateFlow<List<Area>> = _areas

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
        loadOperarios()
        loadCargos()
        loadAreas()
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
                        syncOperarios()
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

    private fun loadOperarios() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                operarioRepository.getAllOperarios().collect { operarioList ->
                    _operarios.value = operarioList
                }
            } catch (e: Exception) {
                _error.value = "Error al cargar operarios: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadCargos() {
        viewModelScope.launch {
            try {
                cargoRepository.getAllCargos().collect { cargoList ->
                    _cargos.value = cargoList
                }
            } catch (e: Exception) {
                _error.value = "Error al cargar cargos: ${e.message}"
            }
        }
    }

    private fun loadAreas() {
        viewModelScope.launch {
            try {
                areaRepository.getAllAreas().collect { areaList ->
                    _areas.value = areaList
                }
            } catch (e: Exception) {
                _error.value = "Error al cargar áreas: ${e.message}"
            }
        }
    }

    private fun loadFincas() {
        viewModelScope.launch {
            try {
                fincaRepository.getAllFincas().collect { fincaList ->
                    _fincas.value = fincaList
                }
            } catch (e: Exception) {
                _error.value = "Error al cargar fincas: ${e.message}"
            }
        }
    }

    fun insertOperario(operario: Operario) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                operarioRepository.insertOperario(operario)

                if (_isOnline.value) {
                    syncOperarios()
                }
            } catch (e: Exception) {
                _error.value = "Error al insertar operario: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateOperario(operario: Operario) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                operarioRepository.updateOperario(operario)

                if (_isOnline.value) {
                    syncOperarios()
                }
            } catch (e: Exception) {
                _error.value = "Error al actualizar operario: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteOperario(operario: Operario) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                operarioRepository.deleteOperario(operario)

                if (_isOnline.value) {
                    syncOperarios()
                }
            } catch (e: Exception) {
                _error.value = "Error al eliminar operario: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun syncOperarios() {
        try {
            operarioRepository.syncOperarios()
            loadOperarios() // Recargar los operarios después de la sincronización
        } catch (e: Exception) {
            _error.value = "Error en la sincronización: ${e.message}"
        }
    }

    fun performFullSync() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val success = operarioRepository.fullSync()
                if (success) {
                    loadOperarios() // Recargar los operarios después de la sincronización completa
                    loadCargos() // También recargamos los datos relacionados
                    loadAreas()
                    loadFincas()
                }
            } catch (e: Exception) {
                _error.value = "Error en la sincronización completa: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAllOperarios() = viewModelScope.launch {
        try {
            _isLoading.value = true
            _error.value = null
            operarioRepository.deleteAllOperarios()

            if (_isOnline.value) {
                syncOperarios()
            }
        } catch (e: Exception) {
            _error.value = "Error al eliminar todos los operarios: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    fun checkOperariosWithCargo(cargoId: Long): Boolean {
        return _operarios.value.any { it.cargoId.toLong() == cargoId }
    }
}