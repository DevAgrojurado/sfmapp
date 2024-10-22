package com.agrojurado.sfmappv2.presentation.ui.admin.operarios

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OperariosViewModel @Inject constructor(
    private val operarioRepository: OperarioRepository,
    private val cargoRepository: CargoRepository,
    private val areaRepository: AreaRepository,
    private val fincaRepository: FincaRepository
) : ViewModel() {

    val operarios = operarioRepository.getAllOperarios().asLiveData()
    val cargos: LiveData<List<Cargo>> = cargoRepository.getAllCargos().asLiveData()
    val areas: LiveData<List<Area>> = areaRepository.getAllAreas().asLiveData()
    val fincas: LiveData<List<Finca>> = fincaRepository.getAllFincas().asLiveData()

    fun insertOperario(operario: Operario) = viewModelScope.launch {
        operarioRepository.insertOperario(operario)
    }

    fun updateOperario(operario: Operario) = viewModelScope.launch {
        operarioRepository.updateOperario(operario)
    }

    fun checkOperariosWithCargo(cargoId: Long): Boolean {
        return operarios.value?.any { it.cargoId.toLong() == cargoId } == true
    }

    fun deleteOperario(operario: Operario) = viewModelScope.launch {
        operarioRepository.deleteOperario(operario)
    }

    fun deleteAllOperarios() = viewModelScope.launch {
        operarioRepository.deleteAllOperarios()
    }
}