package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.shared

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrojurado.sfmappv2.domain.model.Lote
import com.agrojurado.sfmappv2.domain.model.Operario
import com.agrojurado.sfmappv2.domain.repository.LoteRepository
import com.agrojurado.sfmappv2.domain.repository.OperarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.apache.poi.util.LittleEndian.putInt
import javax.inject.Inject

/**
 * Shared ViewModel to manage common selection state between fragments/activities,
 * specifically for Polinizador (Operario), Lote, and Seccion data.
 */
@HiltViewModel
class SharedSelectionViewModel @Inject constructor(
    private val operarioRepository: OperarioRepository,
    private val loteRepository: LoteRepository
) : ViewModel() {

    // ---- Operario (Polinizador) related states ----
    private val _operarios = MutableLiveData<List<Pair<String, Operario>>>()
    val operarios: LiveData<List<Pair<String, Operario>>> = _operarios

    private val _operarioMap = MutableLiveData<Map<Int, String>>()
    val operarioMap: LiveData<Map<Int, String>> = _operarioMap

    private val _selectedOperarioId = MutableLiveData<Int?>()
    val selectedOperarioId: LiveData<Int?> = _selectedOperarioId

    private val _selectedOperario = MutableLiveData<Operario?>()
    val selectedOperario: LiveData<Operario?> = _selectedOperario

    // ---- Lote related states ----
    private val _lotes = MutableLiveData<List<Pair<String, Lote>>>()
    val lotes: LiveData<List<Pair<String, Lote>>> = _lotes

    private val _loteMap = MutableLiveData<Map<Int, String>>()
    val loteMap: LiveData<Map<Int, String>> = _loteMap

    private val _selectedLoteId = MutableLiveData<Int?>()
    val selectedLoteId: LiveData<Int?> = _selectedLoteId

    private val _selectedLote = MutableLiveData<Lote?>()
    val selectedLote: LiveData<Lote?> = _selectedLote

    // ---- Seccion related states ----
    private val _selectedSeccion = MutableLiveData<Int?>()
    val selectedSeccion: LiveData<Int?> = _selectedSeccion

    // ---- Status & Error handling ----
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    init {

        _selectedOperarioId.value = null
        _selectedLoteId.value = null
        _selectedSeccion.value = null

        loadOperarioMap()
        loadLoteMap()
    }

    // ---- Public methods for loading data ----
    fun loadOperarios() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                operarioRepository.getAllOperarios().collectLatest { operariosList ->
                    _operarios.value = operariosList.map { operario ->
                        "${operario.codigo} - ${operario.nombre}" to operario
                    }
                    // If we have a selected ID but no selected operario object, try to find it
                    updateSelectedOperarioIfNeeded()
                }
            } catch (e: Exception) {
                _error.value = "Error al cargar operarios: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadLotes() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                loteRepository.getAllLotes().collectLatest { lotesList ->
                    _lotes.value = lotesList.map { lote ->
                        "Lote ${lote.descripcion}" to lote
                    }
                    // If we have a selected ID but no selected lote object, try to find it
                    updateSelectedLoteIfNeeded()
                }
            } catch (e: Exception) {
                _error.value = "Error al cargar lotes: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }


    // ---- Selection setters ----
    fun setSelectedOperario(operario: Operario?) {
        _selectedOperario.value = operario
        _selectedOperarioId.value = operario?.id
    }

    fun setSelectedOperarioId(operarioId: Int?) {
        _selectedOperarioId.value = operarioId
        // Try to update the selected operario object based on this ID
        updateSelectedOperarioIfNeeded()
    }

    fun setSelectedLote(lote: Lote?) {
        _selectedLote.value = lote
        _selectedLoteId.value = lote?.id
    }

    fun setSelectedLoteId(loteId: Int?) {
        _selectedLoteId.value = loteId
        // Try to update the selected lote object based on this ID
        updateSelectedLoteIfNeeded()
    }

    fun setSelectedSeccion(seccion: Int?) {
        _selectedSeccion.value = seccion
    }

    // ---- Private helper methods ----
    private fun loadOperarioMap() {
        viewModelScope.launch {
            operarioRepository.getAllOperarios().collectLatest { operariosList ->
                _operarioMap.value = operariosList.associate { it.id to it.nombre }
            }
        }
    }

    private fun loadLoteMap() {
        viewModelScope.launch {
            loteRepository.getAllLotes().collectLatest { lotesList ->
                _loteMap.value = lotesList.associate { it.id to (it.descripcion ?: "") }
            }
        }
    }

    private fun updateSelectedOperarioIfNeeded() {
        val operarioId = _selectedOperarioId.value ?: return
        val operariosList = _operarios.value ?: return

        if (_selectedOperario.value == null || _selectedOperario.value?.id != operarioId) {
            _selectedOperario.value = operariosList.find { it.second.id == operarioId }?.second
        }
    }

    private fun updateSelectedLoteIfNeeded() {
        val loteId = _selectedLoteId.value ?: return
        val lotesList = _lotes.value ?: return

        if (_selectedLote.value == null || _selectedLote.value?.id != loteId) {
            _selectedLote.value = lotesList.find { it.second.id == loteId }?.second
        }
    }

    fun ensureDataLoaded() {
        if (_operarios.value.isNullOrEmpty()) {
            loadOperarios()
        }
        if (_lotes.value.isNullOrEmpty()) {
            loadLotes()
        }
    }

    // Método para garantizar que las selecciones se aplican correctamente
    fun applySelections(
        operarioId: Int?,
        loteId: Int?,
        seccion: Int?
    ) {
        viewModelScope.launch {
            // Aplicamos en orden para evitar condiciones de carrera
            operarioId?.let { setSelectedOperarioId(it) }
            loteId?.let { setSelectedLoteId(it) }
            seccion?.let { setSelectedSeccion(it) }
        }
    }

    // Método para obtener valores actuales como Bundle para transferencia entre activities
    fun getCurrentSelections(): Bundle {
        return Bundle().apply {
            putInt("operarioId", _selectedOperarioId.value ?: -1)
            putInt("loteId", _selectedLoteId.value ?: -1)
            putInt("seccion", _selectedSeccion.value ?: -1)
        }
    }

    // ---- Error handling ----
    fun clearError() {
        _error.value = null
    }

    /**
     * Clears the current selections for Operario, Lote, and Seccion.
     * Call this after an operation (like saving) to reset the state for the next entry.
     */
    fun clearSelections() {
        _selectedOperarioId.value = null
        _selectedLoteId.value = null
        _selectedSeccion.value = null
        // Also clear the object references if needed
        _selectedOperario.value = null
        _selectedLote.value = null
        // Log that selections are cleared
        Log.d("SharedSelectionVM", "Selections cleared.")
    }
}