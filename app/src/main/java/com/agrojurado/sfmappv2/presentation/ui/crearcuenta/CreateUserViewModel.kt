package com.agrojurado.sfmappv2.presentation.ui.crearcuenta

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.agrojurado.sfmappv2.domain.model.Area
import com.agrojurado.sfmappv2.domain.model.Cargo
import com.agrojurado.sfmappv2.domain.model.Finca
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.domain.repository.AreaRepository
import com.agrojurado.sfmappv2.domain.repository.CargoRepository
import com.agrojurado.sfmappv2.domain.repository.FincaRepository
import com.agrojurado.sfmappv2.domain.repository.UsuarioRepository
import com.agrojurado.sfmappv2.domain.usecase.usuario.InsertUserAccountUseCase
import com.agrojurado.sfmappv2.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class   CreateUserViewModel @Inject constructor(
    private val insertUserAccountUseCase: InsertUserAccountUseCase,

    private val usuarioRepository: UsuarioRepository,
    private val cargoRepository: CargoRepository,
    private val areaRepository: AreaRepository,
    private val fincasRepository: FincaRepository
) : ViewModel() {

    private val _uiStateGrabar = MutableLiveData<UiState<Usuario?>?>()
    val uiStateGrabar: LiveData<UiState<Usuario?>?> = _uiStateGrabar

    val cargos: LiveData<List<Cargo>> = cargoRepository.getAllCargos().asLiveData()
    val areas: LiveData<List<Area>> = areaRepository.getAllAreas().asLiveData()
    val fincas: LiveData<List<Finca>> = fincasRepository.getAllFincas().asLiveData()

    fun resetUiStateGrabar() {
        _uiStateGrabar.value = null
    }

    fun grabarCuenta(usuario: Usuario) = viewModelScope.launch {
        _uiStateGrabar.value = UiState.Loading

        try {
            val insertedUserId = usuarioRepository.insert(usuario)
            usuario.id = insertedUserId.toLong().toInt()
            _uiStateGrabar.value = UiState.Success(usuario)
        } catch (e: Exception) {
            _uiStateGrabar.value = UiState.Error(e.message ?: "Error al crear usuario")
        }
        }
}