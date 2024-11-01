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
import com.agrojurado.sfmappv2.domain.model.Lote
import com.agrojurado.sfmappv2.domain.repository.LoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class EvaluacionViewModel @Inject constructor(
    private val usuarioRepository: UsuarioRepository,
    private val operarioRepository: OperarioRepository,
    private val loteRepository: LoteRepository,
    private val evaluacionRepository: EvaluacionPolinizacionRepository
) : ViewModel() {

    private val _evaluacionesPorSemana = MutableLiveData<Map<Int, List<EvaluacionPolinizacion>>>()
    val evaluacionesPorSemana: LiveData<Map<Int, List<EvaluacionPolinizacion>>> = _evaluacionesPorSemana

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _loggedInUser = MutableLiveData<Usuario?>()
    val loggedInUser: LiveData<Usuario?> = _loggedInUser

    private val _evaluador = MutableLiveData<Map<Int, String>>()
    val evaluador: LiveData<Map<Int, String>> = _evaluador

    private val _operarios = MutableLiveData<List<Pair<String, Operario>>>()
    val operarios: LiveData<List<Pair<String, Operario>>> = _operarios

    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    private val _evaluaciones = MutableLiveData<List<EvaluacionPolinizacion>>()
    val evaluaciones: LiveData<List<EvaluacionPolinizacion>> = _evaluaciones

    private val _operarioMap = MutableLiveData<Map<Int, String>>()
    val operarioMap: LiveData<Map<Int, String>> = _operarioMap

    private val _lastUsedOperarioId = MutableLiveData<Int?>()
    val lastUsedOperarioId: LiveData<Int?> = _lastUsedOperarioId

    private val _totalPalmas = MutableLiveData<Int>()
    val totalPalmas: LiveData<Int> = _totalPalmas

    private val _currentWeek = MutableLiveData<Int>()
    val currentWeek: LiveData<Int> = _currentWeek

    private val _palmExists = MutableLiveData<Boolean>()
    val palmExists: LiveData<Boolean> = _palmExists

    private val _ubicacion = MutableLiveData<String>()
    val ubicacion: LiveData<String> = _ubicacion

    private val _inflorescencia = MutableLiveData<Int>()

    private val _lotes = MutableLiveData<List<Pair<String, Lote>>>()
    val lotes: LiveData<List<Pair<String, Lote>>> = _lotes

    private val _lastUsedLoteId = MutableLiveData<Int?>()
    val lastUsedLoteId: LiveData<Int?> = _lastUsedLoteId

    init {
        loadLoggedInUser()
        loadOperarioMap()
        loadEvaluadorMap()
        loadLastUsedOperario()
        loadLastUsedLote()
        setCurrentWeek()
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
                selectLastUsedOperario()
            }
        }
    }

    fun loadLotes() {
        viewModelScope.launch {
            loteRepository.getAllLotes().collectLatest { lotesList ->
                _lotes.value = lotesList.map { lote ->
                    "Lote ${lote.descripcion}" to lote
                }
                selectLastUsedLote()
            }
        }
    }

    private fun selectLastUsedOperario() {
        val lastUsedId = _lastUsedOperarioId.value
        val operariosList = _operarios.value
        if (lastUsedId != null && operariosList != null) {
            val position = operariosList.indexOfFirst { it.second.id == lastUsedId }
            if (position != -1) {
                _lastUsedOperarioId.value = lastUsedId
            }
        }
    }

    private fun selectLastUsedLote() {
        val lastUsedId = _lastUsedLoteId.value
        val lotesList = _lotes.value
        if (lastUsedId != null && lotesList != null) {
            val position = lotesList.indexOfFirst { it.second.id == lastUsedId }
            if (position != -1) {
                _lastUsedLoteId.value = lastUsedId
            }
        }
    }

    private fun setCurrentWeek() {
        val calendar = Calendar.getInstance()
        _currentWeek.value = calendar.get(Calendar.WEEK_OF_YEAR)
    }

    fun updateTotalPalmas(idPolinizador: Int) {
        viewModelScope.launch {
            val week = _currentWeek.value ?: return@launch
            evaluacionRepository.getEvaluaciones().collectLatest { evaluaciones ->
                val total = evaluaciones.count {
                    it.idPolinizador == idPolinizador && it.semana == week
                }
                _totalPalmas.value = total
                Log.d("EvaluacionViewModel", "Total palmas para polinizador $idPolinizador en semana $week: $total")
            }
        }
    }

    fun loadEvaluacionesPorSemana() {
        viewModelScope.launch {
            evaluacionRepository.getEvaluaciones().collectLatest { evaluacionesList ->
                val groupedEvaluaciones = evaluacionesList.groupBy { it.semana }
                _evaluacionesPorSemana.value = groupedEvaluaciones
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

    private fun loadEvaluadorMap() {
        viewModelScope.launch {
            usuarioRepository.getAllUsersUseCase().collectLatest { usuariosList ->
                _evaluador.value = usuariosList.associate { it.id to it.nombre }
            }
        }
    }

    private fun loadLastUsedOperario() {
        viewModelScope.launch {
            try {
                val lastEvaluacion = evaluacionRepository.getLastEvaluacion()
                _lastUsedOperarioId.value = lastEvaluacion?.idPolinizador
                Log.d("EvaluacionViewModel", "Último operario cargado: ${lastEvaluacion?.idPolinizador}")
            } catch (e: Exception) {
                Log.e("EvaluacionViewModel", "Error al cargar el último operario: ${e.message}")
            }
        }
    }

    private fun loadLastUsedLote() {
        viewModelScope.launch {
            try {
                val lastEvaluacion = evaluacionRepository.getLastEvaluacion()
                _lastUsedLoteId.value = lastEvaluacion?.idlote
                Log.d("EvaluacionViewModel", "Último lote cargado: ${lastEvaluacion?.idlote}")
            } catch (e: Exception) {
                Log.e("EvaluacionViewModel", "Error al cargar el último lote: ${e.message}")
            }
        }
    }

    fun checkPalmExists(semana: Int?, lote: Int?, palma: Int?, idPolinizador: Int?) {
        viewModelScope.launch {
            try {
                if (semana == null || lote == null || palma == null || idPolinizador == null) {
                    _palmExists.value = false
                    return@launch
                }

                val exists = evaluacionRepository.checkPalmExists(semana, lote, palma, idPolinizador)
                _palmExists.value = exists
                Log.d("EvaluacionViewModel", "Palma existente: $exists (Semana: $semana, Lote: $lote, Palma: $palma, IdPolinizador: $idPolinizador)")
            } catch (e: Exception) {
                Log.e("EvaluacionViewModel", "Error al verificar la palma: ${e.message}")
                _errorMessage.value = "Error al verificar la palma: ${e.message}"
                _palmExists.value = false
            }
        }
    }

    fun setInflorescencia(value: Int) {
        _inflorescencia.value = value
    }

    fun setUbicacion(ubicacion: String) {
        _ubicacion.value = ubicacion
    }

    fun saveAllData(
        informacionGeneral: Map<String, Any?>,
        detallesPolinizacion: Map<String, Any?>,
        evaluacion: Map<String, Any?>
    ) {
        viewModelScope.launch {
            try {
                val requiredFields = listOf("etFecha", "etHora", "etSemana", "spinnerPolinizador", "spinnerLote", "etSeccion", "etPalma")
                for (field in requiredFields) {
                    if (informacionGeneral[field] == null) {
                        throw IllegalArgumentException("El campo $field es obligatorio")
                    }
                }

                if (_ubicacion.value.isNullOrEmpty()) {
                    throw IllegalArgumentException("La ubicación es obligatoria")
                }

                if (evaluacion["observaciones"] == null || (evaluacion["observaciones"] as? String)?.isEmpty() == true) {
                    throw IllegalArgumentException("Las observaciones son obligatorias")
                }

                val semana = (informacionGeneral["etSemana"] as? Int) ?: throw IllegalArgumentException("Semana inválida")
                val lote = (informacionGeneral["spinnerLote"] as? Int) ?: throw IllegalArgumentException("Lote inválido")
                val palma = (informacionGeneral["etPalma"] as? Int) ?: throw IllegalArgumentException("Palma inválida")
                val idPolinizador = (informacionGeneral["spinnerPolinizador"] as? Int) ?: throw IllegalArgumentException("ID del polinizador inválido")

                // Verificar si la palma ya existe
                val exists = evaluacionRepository.checkPalmExists(semana, lote, palma, idPolinizador)
                if (exists) {
                    _palmExists.value = true
                    _errorMessage.value = "Palma existente"
                    _saveResult.value = false
                    return@launch
                }

                val evaluacionPolinizacion = EvaluacionPolinizacion(
                    fecha = informacionGeneral["etFecha"] as String,
                    hora = informacionGeneral["etHora"] as String,
                    semana = semana,
                    ubicacion = _ubicacion.value ?: "",
                    idEvaluador = _loggedInUser.value?.id ?: throw IllegalArgumentException("ID del evaluador no disponible"),
                    idPolinizador = idPolinizador,
                    idlote = lote,
                    seccion = (informacionGeneral["etSeccion"] as? Int) ?: throw IllegalArgumentException("Sección inválida"),
                    palma = palma,
                    inflorescencia = _inflorescencia.value,
                    antesis = detallesPolinizacion["antesis"] as? Int,
                    postAntesis = detallesPolinizacion["postAntesis"] as? Int,
                    antesisDejadas = detallesPolinizacion["antesisDejadas"] as? Int,
                    postAntesisDejadas = detallesPolinizacion["postAntesisDejadas"] as? Int,
                    espate = evaluacion["espate"] as? Int,
                    aplicacion = evaluacion["aplicacion"] as? Int,
                    marcacion = evaluacion["marcacion"] as? Int,
                    repaso1 = evaluacion["repaso1"] as? Int,
                    repaso2 = evaluacion["repaso2"] as? Int,
                    observaciones = evaluacion["observaciones"] as String
                )

                evaluacionRepository.insertEvaluacion(evaluacionPolinizacion)
                _saveResult.value = true
                _palmExists.value = false
                Log.d("EvaluacionViewModel", "Evaluación guardada exitosamente.")
            } catch (e: Exception) {
                _saveResult.value = false
                Log.e("EvaluacionViewModel", "Error al guardar la evaluación: ${e.message}")
                _errorMessage.value = e.message ?: "Error desconocido al guardar la evaluación"
            }
        }
    }
}