package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import android.widget.Toast
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
import com.agrojurado.sfmappv2.domain.security.RoleAccessControl
import com.agrojurado.sfmappv2.domain.security.UserRoleConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class EvaluacionViewModel @Inject constructor(
    private val usuarioRepository: UsuarioRepository,
    private val operarioRepository: OperarioRepository,
    private val loteRepository: LoteRepository,
    private val evaluacionRepository: EvaluacionPolinizacionRepository,
    private val roleAccessControl: RoleAccessControl,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Estado de sincronización y conectividad
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Estados existentes
    private val _evaluacionesPorSemana = MutableLiveData<Map<Int, List<EvaluacionPolinizacion>>>()
    val evaluacionesPorSemana: LiveData<Map<Int, List<EvaluacionPolinizacion>>> = _evaluacionesPorSemana

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

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

    private val _isSaving = MutableLiveData<Boolean>()
    val isSaving: LiveData<Boolean> = _isSaving

    private val _syncStatus = MutableLiveData<SyncStatus>()
    val syncStatus: LiveData<SyncStatus> = _syncStatus

    sealed class SyncStatus {
        object Idle : SyncStatus()
        object Loading : SyncStatus()
        data class Success(val message: String) : SyncStatus()
        data class Error(val message: String) : SyncStatus()
    }


    private var syncInProgress = false

    init {
        observeNetworkState()
        loadLoggedInUser()
        loadOperarioMap()
        loadEvaluadorMap()
        loadLastUsedOperario()
        loadLastUsedLote()
        setCurrentWeek()
    }

    // Verifica si hay sincronización en curso
    private fun isSyncInProgress(): Boolean {
        return syncInProgress
    }

    private fun observeNetworkState() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                viewModelScope.launch {
                    // Verificar si ya hay una sincronización en curso
                    if (!isSyncInProgress()) {
                        try {
                            syncInProgress = true // Marcar sincronización en curso
                            _isLoading.value = true
                            syncEvaluaciones() // Llamar a la sincronización
                        } catch (e: Exception) {
                            _error.value = "Error al sincronizar: ${e.message}"
                        } finally {
                            _isLoading.value = false
                            syncInProgress = false // Marcar sincronización como terminada
                        }
                    }
                }
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback)
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
            try {
                _isLoading.value = true
                operarioRepository.getAllOperarios().collectLatest { operariosList ->
                    _operarios.value = operariosList.map { operario ->
                        "${operario.codigo} - ${operario.nombre}" to operario
                    }
                    selectLastUsedOperario()
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
                    selectLastUsedLote()
                }
            } catch (e: Exception) {
                _error.value = "Error al cargar lotes: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun syncEvaluaciones() {
        try {
            evaluacionRepository.syncEvaluaciones()
            loadEvaluacionesPorSemana()
        } catch (e: Exception) {
            _error.value = "Error en la sincronización: ${e.message}"
        }
    }

    fun performFullSync() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Loading
            try {
                // Tus operaciones de sincronización
                withContext(Dispatchers.IO) {
                    syncEvaluaciones()
                }
                _syncStatus.value = SyncStatus.Success("Sincronización completada")
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error("Error en sincronización: ${e.message}")
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
            try {
                _isLoading.value = true
                evaluacionRepository.getEvaluaciones().collectLatest { evaluacionesList ->
                    // Obtener el usuario actual
                    val currentUser = _loggedInUser.value

                    // Filtrar evaluaciones según el rol del usuario
                    val filteredEvaluaciones = when {
                        // Si es admin o coordinador, mostrar todas las evaluaciones
                        currentUser?.rol?.equals(UserRoleConstants.ROLE_ADMIN, ignoreCase = true) == true ||
                                currentUser?.rol?.equals(UserRoleConstants.ROLE_COORDINATOR, ignoreCase = true) == true -> {
                            evaluacionesList
                        }
                        // Si es evaluador, mostrar solo evaluaciones de su finca
                        currentUser?.rol?.equals(UserRoleConstants.ROLE_EVALUATOR, ignoreCase = true) == true -> {
                            evaluacionesList.filter { evaluacion ->
                                // Verificar si la evaluación pertenece a un operario de la misma finca del evaluador
                                val operariosEnFinca = operarioRepository.getAllOperarios()
                                    .first()
                                    .filter { it.fincaId == currentUser.idFinca }
                                    .map { it.id }

                                operariosEnFinca.contains(evaluacion.idPolinizador)
                            }
                        }
                        // Por defecto, no mostrar evaluaciones
                        else -> emptyList()
                    }

                    // Agrupar evaluaciones filtradas por semana
                    val groupedEvaluaciones = filteredEvaluaciones.groupBy { it.semana }
                    _evaluacionesPorSemana.value = groupedEvaluaciones
                }
            } catch (e: Exception) {
                _error.value = "Error al cargar evaluaciones: ${e.message}"
            } finally {
                _isLoading.value = false
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
                _isLoading.value = true
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
                val fecha = informacionGeneral["etFecha"] as String
                val hora = informacionGeneral["etHora"] as String
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
                    fecha = fecha,
                    hora = hora,
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
                    observaciones = evaluacion["observaciones"] as String,
                )

                evaluacionRepository.insertEvaluacion(evaluacionPolinizacion)
                _saveResult.value = true
                _palmExists.value = false

                Log.d("EvaluacionViewModel", "Evaluación guardada exitosamente.")
            } catch (e: Exception) {
                _saveResult.value = false
                Log.e("EvaluacionViewModel", "Error al guardar la evaluación: ${e.message}")
                _errorMessage.value = e.message ?: "Error desconocido al guardar la evaluación"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteEvaluacion(evaluacion: EvaluacionPolinizacion) {
        viewModelScope.launch {
            val currentUser = _loggedInUser.value
                ?: throw SecurityException("No authenticated user")

            try {
                // Verifica los permisos del usuario
                if (!roleAccessControl.canDeleteEvaluations(currentUser)) {
                    throw SecurityException("User lacks permission to delete evaluations")
                }

                // Si el usuario tiene permisos, proceder con la eliminación
                _isLoading.value = true
                _error.value = null
                evaluacionRepository.deleteEvaluacion(evaluacion)

                // Sincronizar si está en línea
                if (_isOnline.value) {
                    syncEvaluaciones()
                }

                // Recargar evaluaciones por semana
                loadEvaluacionesPorSemana()
                _saveResult.value = true
            } catch (e: SecurityException) {
                // Capturar la SecurityException y mostrar un mensaje de error
                _error.value = e.message
                _saveResult.value = false
                Log.e("EvaluacionViewModel", "Error: ${e.message}")
                showToast("No tienes permisos para eliminar esta evaluación.")
            } catch (e: Exception) {
                // Capturar otras excepciones
                _errorMessage.value = "Error al eliminar la evaluación: ${e.message}"
                _saveResult.value = false
                Log.e("EvaluacionViewModel", "Error: ${e.message}")
                showToast("Ocurrió un error al eliminar la evaluación.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun showToast(message: String) {
        // Mostrar un mensaje en un Toast
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }


    fun deleteAllEvaluaciones() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                evaluacionRepository.deleteAllEvaluaciones()

                // Sincronizar si está en línea
                if (_isOnline.value) {
                    syncEvaluaciones()
                }

                loadEvaluacionesPorSemana()
                _saveResult.value = true
            } catch (e: Exception) {
                _error.value = "Error al eliminar todas las evaluaciones: ${e.message}"
                _saveResult.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Aquí podrías limpiar cualquier recurso si es necesario
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            connectivityManager.unregisterNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {}
            )
        } catch (e: Exception) {
            Log.e("EvaluacionViewModel", "Error al desregistrar network callback: ${e.message}")
        }
    }

    // Limpiar mensaje de error
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}