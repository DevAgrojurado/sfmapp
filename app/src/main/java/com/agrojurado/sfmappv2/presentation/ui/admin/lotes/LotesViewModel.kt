package com.agrojurado.sfmappv2.presentation.ui.admin.lotes

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrojurado.sfmappv2.domain.model.Finca
import com.agrojurado.sfmappv2.domain.model.Lote
import com.agrojurado.sfmappv2.domain.repository.FincaRepository
import com.agrojurado.sfmappv2.domain.repository.LoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LotesViewModel @Inject constructor(
    private val loteRepository: LoteRepository,
    private val fincaRepository: FincaRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _lotes = MutableStateFlow<List<Lote>>(emptyList())
    val lotes: StateFlow<List<Lote>> = _lotes

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
        loadLotes()
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
                        syncLotes()
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

    private fun loadLotes() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                loteRepository.getAllLotes().collect { loteList ->
                    _lotes.value = loteList
                }
            } catch (e: Exception) {
                _error.value = "Error al cargar lotes: ${e.message}"
            } finally {
                _isLoading.value = false
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

    fun insertLote(lote: Lote) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                loteRepository.insertLote(lote)

                if (_isOnline.value) {
                    syncLotes()
                }
            } catch (e: Exception) {
                _error.value = "Error al insertar lote: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateLote(lote: Lote) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                loteRepository.updateLote(lote)

                if (_isOnline.value) {
                    syncLotes()
                }
            } catch (e: Exception) {
                _error.value = "Error al actualizar lote: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteLote(lote: Lote) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                loteRepository.deleteLote(lote)

                if (_isOnline.value) {
                    syncLotes()
                }
            } catch (e: Exception) {
                _error.value = "Error al eliminar lote: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun syncLotes() {
        try {
            loteRepository.syncLotes()
            loadLotes() // Recargar los lotes después de la sincronización
        } catch (e: Exception) {
            _error.value = "Error en la sincronización: ${e.message}"
        }
    }

    fun performFullSync() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val success = loteRepository.fullSync()
                if (success) {
                    loadLotes() // Recargar los lotes después de la sincronización completa
                    loadFincas() // También recargamos las fincas ya que están relacionadas
                }
            } catch (e: Exception) {
                _error.value = "Error en la sincronización completa: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAllLotes() = viewModelScope.launch {
        try {
            _isLoading.value = true
            _error.value = null
            loteRepository.deleteAllLotes()

            if (_isOnline.value) {
                syncLotes()
            }
        } catch (e: Exception) {
            _error.value = "Error al eliminar todos los lotes: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
}