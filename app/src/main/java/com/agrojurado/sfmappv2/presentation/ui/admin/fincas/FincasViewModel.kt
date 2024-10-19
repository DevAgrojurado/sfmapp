package com.agrojurado.sfmappv2.presentation.ui.admin.fincas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.agrojurado.sfmappv2.domain.model.Cargo
import com.agrojurado.sfmappv2.domain.model.Finca
import com.agrojurado.sfmappv2.domain.repository.CargoRepository
import com.agrojurado.sfmappv2.domain.repository.FincaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FincasViewModel @Inject constructor(
    private val repository: FincaRepository
) : ViewModel() {

    val fincas = repository.getAllFincas().asLiveData()

    fun insertFinca(finca: Finca) = viewModelScope.launch {
        repository.insertFinca(finca)
    }

    fun getFincaById(id: Int) = viewModelScope.launch {
        repository.getFincaById(id)
    }

    fun updateFinca(finca: Finca) = viewModelScope.launch {
        repository.updateFinca(finca)
    }

    fun deleteFinca(finca: Finca) = viewModelScope.launch {
        repository.deleteFinca(finca)
    }

    fun deleteAllFincas() = viewModelScope.launch {
        repository.deleteAllFincas()
    }
}