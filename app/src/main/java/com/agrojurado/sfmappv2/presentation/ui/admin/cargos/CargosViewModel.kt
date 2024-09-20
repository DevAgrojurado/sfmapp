package com.agrojurado.sfmappv2.presentation.ui.admin.cargos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.agrojurado.sfmappv2.domain.model.Cargo
import com.agrojurado.sfmappv2.domain.repository.CargoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CargosViewModel @Inject constructor(
    private val repository: CargoRepository
) : ViewModel() {

    val cargos = repository.getAllCargos().asLiveData()

    fun insertCargo(cargo: Cargo) = viewModelScope.launch {
        repository.insertCargo(cargo)
    }

    fun getCargoById(id: Int) = viewModelScope.launch {
        repository.getCargoById(id)
    }

    fun updateCargo(cargo: Cargo) = viewModelScope.launch {
        repository.updateCargo(cargo)
    }

    fun deleteCargo(cargo: Cargo) = viewModelScope.launch {
        repository.deleteCargo(cargo)
    }

    fun deleteAllCargos() = viewModelScope.launch {
        repository.deleteAllCargos()
    }
}