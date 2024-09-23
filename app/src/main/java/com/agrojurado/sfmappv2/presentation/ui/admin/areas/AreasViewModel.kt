package com.agrojurado.sfmappv2.presentation.ui.admin.areas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.agrojurado.sfmappv2.domain.model.Area
import com.agrojurado.sfmappv2.domain.model.Cargo
import com.agrojurado.sfmappv2.domain.repository.AreaRepository
import com.agrojurado.sfmappv2.domain.repository.CargoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AreasViewModel @Inject constructor(
    private val repository: AreaRepository
) : ViewModel() {

    val areas = repository.getAllAreas().asLiveData()

    fun insertArea(area: Area) = viewModelScope.launch {
        repository.insertArea(area)
    }

    fun getAreaById(id: Int) = viewModelScope.launch {
        repository.getAreaById(id)
    }

    fun updateArea(area: Area) = viewModelScope.launch {
        repository.updateArea(area)
    }

    fun deleteArea(area: Area) = viewModelScope.launch {
        repository.deleteArea(area)
    }

    fun deleteAllAreas() = viewModelScope.launch {
        repository.deleteAllAreas()
    }
}