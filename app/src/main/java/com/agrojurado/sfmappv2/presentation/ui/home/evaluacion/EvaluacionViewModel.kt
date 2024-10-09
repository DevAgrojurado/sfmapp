package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrojurado.sfmappv2.domain.repository.UsuarioRepository
import com.agrojurado.sfmappv2.domain.repository.OperarioRepository
import com.agrojurado.sfmappv2.domain.repository.EvaluacionPolinizacionRepository
import com.agrojurado.sfmappv2.domain.model.Operario
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EvaluacionViewModel @Inject constructor(
    private val usuarioRepository: UsuarioRepository,
    private val operarioRepository: OperarioRepository,
    private val evaluacionRepository: EvaluacionPolinizacionRepository
) : ViewModel() {

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _loggedInUser = MutableLiveData<Usuario?>()
    val loggedInUser: LiveData<Usuario?> = _loggedInUser

    private val _operarios = MutableLiveData<List<Pair<String, Operario>>>()
    val operarios: LiveData<List<Pair<String, Operario>>> = _operarios

    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    private val _evaluaciones = MutableLiveData<List<EvaluacionPolinizacion>>()
    val evaluaciones: LiveData<List<EvaluacionPolinizacion>> = _evaluaciones

    private val _operarioMap = MutableLiveData<Map<Int, String>>()
    val operarioMap: LiveData<Map<Int, String>> = _operarioMap

    private val _inflorescencia = MutableLiveData<Int>()
    val inflorescencia: LiveData<Int> = _inflorescencia

    init {
        loadLoggedInUser()
        loadOperarioMap()
    }

    fun loadEvaluaciones() {
        viewModelScope.launch {
            evaluacionRepository.getEvaluaciones().collectLatest { evaluacionesList ->
                _evaluaciones.value = evaluacionesList
            }
        }
    }

    private fun loadLoggedInUser() {
        viewModelScope.launch {
            try {
                val email = usuarioRepository.getLoggedInUserEmail()
                email?.let {
                    usuarioRepository.getUserByEmail(it).collectLatest { user ->
                        _loggedInUser.value = user
                        Log.d("EvaluacionViewModel", "Usuario cargado: $user")
                    }
                } ?: run {
                    Log.d("EvaluacionViewModel", "No se encontró usuario logueado")
                }
            } catch (e: Exception) {
                Log.e("EvaluacionViewModel", "Error al cargar usuario: ${e.message}")
            }
        }
    }

    fun loadOperarios() {
        viewModelScope.launch {
            operarioRepository.getAllOperarios().collectLatest { operariosList ->
                _operarios.value = operariosList.map { operario ->
                    "${operario.codigo} - ${operario.nombre}" to operario
                }
            }
        }
    }

    private fun loadOperarioMap() {
        viewModelScope.launch {
            operarioRepository.getAllOperarios().collectLatest { operariosList ->
                _operarioMap.value = operariosList.associate { it.id to it.nombre }
            }
        }
    }

    fun setInflorescencia(value: Int) {
        _inflorescencia.value = value
    }

    fun saveAllData(
        informacionGeneral: Map<String, Any>,
        detallesPolinizacion: Map<String, Any>,
        evaluacion: Map<String, Any>
    ) {
        viewModelScope.launch {
            try {
                val evaluacionPolinizacion = EvaluacionPolinizacion(
                    fecha = informacionGeneral["etFecha"] as String,
                    hora = informacionGeneral["etHora"] as String,
                    semana = informacionGeneral["etSemana"] as Int,
                    idEvaluador = _loggedInUser.value?.id ?: throw IllegalArgumentException("ID del evaluador no disponible"),
                    idPolinizador = informacionGeneral["spinnerPolinizador"] as Int,
                    lote = informacionGeneral["etLote"] as Int,
                    inflorescencia = _inflorescencia.value ?: 0,
                    antesis = detallesPolinizacion["antesis"] as Int,
                    postAntesis = detallesPolinizacion["postAntesis"] as Int,
                    antesisDejadas = detallesPolinizacion["antesisDejadas"] as Int,
                    postAntesisDejadas = detallesPolinizacion["postAntesisDejadas"] as Int,
                    espate = evaluacion["espate"] as Int,
                    aplicacion = evaluacion["aplicacion"] as Int,
                    marcacion = evaluacion["marcacion"] as Int,
                    repaso1 = evaluacion["repaso1"] as Int,
                    repaso2 = evaluacion["repaso2"] as Int,
                    observaciones = evaluacion["observaciones"] as String
                )

                evaluacionRepository.insertEvaluacion(evaluacionPolinizacion)
                _saveResult.value = true
                Log.d("EvaluacionViewModel", "Evaluación guardada exitosamente. Inflorescencia: ${evaluacionPolinizacion.inflorescencia}")
            } catch (e: Exception) {
                _saveResult.value = false
                Log.e("EvaluacionViewModel", "Error al guardar evaluación: ${e.message}")
                _errorMessage.value = e.message ?: "Error desconocido al guardar la evaluación"
            }
        }
    }
}