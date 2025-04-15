package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluacionfragmentsform

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
import com.agrojurado.sfmappv2.domain.repository.EvaluacionGeneralRepository
import com.agrojurado.sfmappv2.domain.repository.LoteRepository
import com.agrojurado.sfmappv2.domain.security.RoleAccessControl
import com.agrojurado.sfmappv2.domain.security.UserRoleConstants
import com.agrojurado.sfmappv2.data.sync.SyncStatus
import com.agrojurado.sfmappv2.utils.EvaluacionPdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    private val evaluacionGeneralRepository: EvaluacionGeneralRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var evaluacionGeneralId: Int? = null

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

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

    private val _loteMap = MutableLiveData<Map<Int, String>>()
    val loteMap: LiveData<Map<Int, String>> = _loteMap

    private val _lastUsedLoteId = MutableLiveData<Int?>()
    val lastUsedLoteId: LiveData<Int?> = _lastUsedLoteId

    private val _isSaving = MutableLiveData<Boolean>()
    val isSaving: LiveData<Boolean> = _isSaving

    private val _validationErrors = MutableLiveData<List<String>>()
    val validationErrors: LiveData<List<String>> = _validationErrors

    private val _currentPage = MutableLiveData<Int>(0)
    val currentPage: LiveData<Int> = _currentPage

    private var cachedEvaluaciones: Map<Int, List<EvaluacionPolinizacion>>? = null
    private var cachedOperariosIds: Set<Int>? = null

    init {
        observeNetworkState()
        viewModelScope.launch {
            loadLoggedInUser()
            loadOperarioMap()
            loadEvaluadorMap()
            loadLastUsedOperario()
            loadLastUsedLote()
            setCurrentWeek()
            loadLoteMap()
        }
    }

    fun setCurrentPage(page: Int) {
        _currentPage.value = page
    }

    fun setEvaluacionGeneralId(id: Int?) {
        evaluacionGeneralId = id
        Log.d("EvaluacionViewModel", "Evaluacion General ID asignada: $evaluacionGeneralId")
    }

    fun getEvaluacionGeneralId(): Int? = evaluacionGeneralId

    private fun observeNetworkState() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
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

    private fun loadLoteMap() {
        viewModelScope.launch {
            loteRepository.getAllLotes().collectLatest { lotesList ->
                _loteMap.value = lotesList.associate { it.id to (it.descripcion ?: "") }
            }
        }
    }

    fun loadEvaluacionesPorSemana() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                cachedEvaluaciones?.let {
                    _evaluacionesPorSemana.value = it
                    _isLoading.value = false
                    return@launch
                }

                evaluacionRepository.getEvaluaciones()
                    .map { evaluacionesList ->
                        filterEvaluacionesByRole(_loggedInUser.value, evaluacionesList)
                            .groupBy { it.semana }
                    }
                    .distinctUntilChanged()
                    .collectLatest { groupedEvaluaciones ->
                        cachedEvaluaciones = groupedEvaluaciones
                        _evaluacionesPorSemana.value = groupedEvaluaciones
                    }
            } catch (e: Exception) {
                _error.value = "Error al cargar evaluaciones: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearCache() {
        cachedEvaluaciones = null
    }

    private suspend fun filterEvaluacionesByRole(user: Usuario?, evaluacionesList: List<EvaluacionPolinizacion>): List<EvaluacionPolinizacion> {
        return when {
            user?.rol?.equals(UserRoleConstants.ROLE_ADMIN, ignoreCase = true) == true ||
                    user?.rol?.equals(UserRoleConstants.ROLE_COORDINATOR, ignoreCase = true) == true -> {
                evaluacionesList
            }
            user?.rol?.equals(UserRoleConstants.ROLE_EVALUATOR, ignoreCase = true) == true -> {
                val operariosIds = cachedOperariosIds ?: run {
                    operarioRepository.getAllOperarios()
                        .first()
                        .filter { it.fincaId == user.idFinca }
                        .map { it.id }
                        .toSet()
                        .also { cachedOperariosIds = it }
                }
                evaluacionesList.filter { evaluacion ->
                    operariosIds.contains(evaluacion.idPolinizador)
                }
            }
            else -> emptyList()
        }
    }

    private fun loadOperarioMap() {
        viewModelScope.launch {
            operarioRepository.getAllOperarios().collectLatest { operariosList ->
                _operarioMap.value = operariosList.associate { it.id to it.nombre }
                clearOperariosCache()
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

    fun checkPalmExists(
        semana: Int?,
        lote: Int?,
        palma: Int?,
        idPolinizador: Int?,
        seccion: Int?,
        evaluacionGeneralId: Int?
    ) {
        viewModelScope.launch {
            try {
                if (semana == null || lote == null || palma == null || idPolinizador == null || seccion == null || evaluacionGeneralId == null) {
                    _palmExists.value = false
                    return@launch
                }

                val exists = evaluacionRepository.checkPalmExists(
                    semana = semana,
                    lote = lote,
                    palma = palma,
                    idPolinizador = idPolinizador,
                    seccion = seccion,
                    evaluacionGeneralId = evaluacionGeneralId
                )
                _palmExists.value = exists
                Log.d("EvaluacionViewModel", "Palma existente: $exists (Semana: $semana, Lote: $lote, Palma: $palma, Seccion: $seccion, IdPolinizador: $idPolinizador, EvaluacionGeneralId: $evaluacionGeneralId)")
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
                _isSaving.value = true
                _errorMessage.value = null
                _saveResult.value = false

                val errorFields = validateRequiredFields(informacionGeneral, evaluacion)
                if (errorFields.isNotEmpty()) {
                    _validationErrors.value = errorFields
                    _isSaving.value = false
                    return@launch
                }

                val fecha = informacionGeneral["etFecha"] as String
                val hora = informacionGeneral["etHora"] as String
                val semana = (informacionGeneral["etSemana"] as? Int)
                    ?: throw IllegalArgumentException("Semana inválida")
                val lote = (informacionGeneral["spinnerLote"] as? Int)
                    ?: throw IllegalArgumentException("Lote inválido")
                val palma = informacionGeneral["etPalma"] as? Int
                val idPolinizador = (informacionGeneral["spinnerPolinizador"] as? Int)
                    ?: throw IllegalArgumentException("ID del polinizador inválido")
                val seccion = (informacionGeneral["etSeccion"] as? Int)
                    ?: throw IllegalArgumentException("Sección inválida")

                val evalGeneralId = evaluacionGeneralId
                if (evalGeneralId == null) {
                    _errorMessage.value = "No hay una evaluación general activa"
                    Log.e("EvaluacionViewModel", "Error: evaluacionGeneralId es null")
                    _isSaving.value = false
                    return@launch
                }

                val evaluacionPolinizacion = createEvaluacionPolinizacion(
                    fecha, hora, semana, lote, palma, idPolinizador, seccion,
                    informacionGeneral, detallesPolinizacion, evaluacion
                ).copy(evaluacionGeneralId = evalGeneralId)

                val insertedId = evaluacionRepository.insertEvaluacion(evaluacionPolinizacion)
                Log.d("EvaluacionViewModel", "✅ Saved EvaluacionPolinizacion with ID: $insertedId, evaluacionGeneralId: $evalGeneralId")
                updateAfterSuccessfulSave(idPolinizador, lote, evalGeneralId)
                _saveResult.value = true

            } catch (e: Exception) {
                _errorMessage.value = "Error al guardar evaluación individual: ${e.message}"
                Log.e("EvaluacionViewModel", "Error: ${e.message}", e)
                handleSaveError(e)
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun validateRequiredFields(
        informacionGeneral: Map<String, Any?>,
        evaluacion: Map<String, Any?>
    ): List<String> {
        val requiredGeneralFields = listOf(
            "etFecha" to "Fecha",
            "etHora" to "Hora",
            "etSemana" to "Semana",
            "spinnerPolinizador" to "Polinizador",
            "spinnerLote" to "Lote",
            "etSeccion" to "Sección"
        )

        val errorFields = mutableListOf<String>()

        requiredGeneralFields.forEach { (key, fieldName) ->
            if (informacionGeneral[key] == null) {
                errorFields.add(key)
            }
        }

        if (_ubicacion.value.isNullOrEmpty()) {
            errorFields.add("ubicacion")
        }

        if (evaluacion["observaciones"] == null ||
            (evaluacion["observaciones"] as? String)?.isEmpty() == true) {
            errorFields.add("observaciones")
        }

        return errorFields
    }

    private fun createEvaluacionPolinizacion(
        fecha: String, hora: String, semana: Int, lote: Int,
        palma: Int?, idPolinizador: Int, seccion: Int,
        informacionGeneral: Map<String, Any?>,
        detallesPolinizacion: Map<String, Any?>,
        evaluacion: Map<String, Any?>
    ): EvaluacionPolinizacion = EvaluacionPolinizacion(
        id = 0,
        serverId = null,
        fecha = fecha,
        hora = hora,
        semana = semana,
        ubicacion = _ubicacion.value ?: "",
        idEvaluador = _loggedInUser.value?.id
            ?: throw IllegalArgumentException("ID del evaluador no disponible"),
        idPolinizador = idPolinizador,
        idlote = lote,
        seccion = seccion,
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
        isSynced = false,
        evaluacionGeneralId = null, // Este valor se establece con .copy() en saveAllData
        timestamp = System.currentTimeMillis()
    )

    private fun updateAfterSuccessfulSave(idPolinizador: Int, lote: Int, evaluacionGeneralId: Int) {
        updateTotalPalmas(idPolinizador, evaluacionGeneralId)
        clearCache()
        loadEvaluacionesPorSemana()
        _lastUsedLoteId.value = lote
        _lastUsedOperarioId.value = idPolinizador
    }

    fun deleteEvaluacion(evaluacion: EvaluacionPolinizacion) {
        viewModelScope.launch {
            val currentUser = _loggedInUser.value
                ?: throw SecurityException("No authenticated user")

            try {
                if (!roleAccessControl.canDeleteEvaluations(currentUser)) {
                    throw SecurityException("User lacks permission to delete evaluations")
                }

                _isLoading.value = true
                _error.value = null
                evaluacionRepository.deleteEvaluacion(evaluacion)

                loadEvaluacionesPorSemana()
                _saveResult.value = true
            } catch (e: SecurityException) {
                _error.value = e.message
                _saveResult.value = false
                Log.e("EvaluacionViewModel", "Error: ${e.message}")
                showToast("No tienes permisos para eliminar esta evaluación.")
            } catch (e: Exception) {
                _errorMessage.value = "Error al eliminar la evaluación: ${e.message}"
                _saveResult.value = false
                Log.e("EvaluacionViewModel", "Error: ${e.message}")
                showToast("Ocurrió un error al eliminar la evaluación.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun exportToPdf(context: Context) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val evaluacionesList = evaluacionRepository.getEvaluaciones().first()
                val evaluadorMap = _evaluador.value ?: emptyMap()
                val operarioMap = _operarioMap.value ?: emptyMap()
                val loteMap = _loteMap.value ?: emptyMap()

                val pdfGenerator = EvaluacionPdfGenerator(context)
                val pdfFile = pdfGenerator.generatePdf(
                    evaluaciones = evaluacionesList,
                    evaluadorMap = evaluadorMap,
                    operarioMap = operarioMap,
                    loteMap = loteMap
                )

                _saveResult.value = true
                showToast("PDF generado exitosamente: ${pdfFile.name}")
            } catch (e: Exception) {
                _errorMessage.value = "Error al generar PDF: ${e.message}"
                _saveResult.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun clearOperariosCache() {
        cachedOperariosIds = null
    }

    override fun onCleared() {
        super.onCleared()
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            connectivityManager.unregisterNetworkCallback(object : ConnectivityManager.NetworkCallback() {})
        } catch (e: Exception) {
            Log.e("EvaluacionViewModel", "Error al desregistrar network callback: ${e.message}")
        }
        clearOperariosCache()
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    private fun handleSaveError(e: Exception) {
        _errorMessage.value = "Error al guardar la evaluación"
        _saveResult.value = false
    }

    fun updateTotalPalmas(idPolinizador: Int, evaluacionGeneralId: Int?) {
        viewModelScope.launch {
            val week = _currentWeek.value ?: return@launch
            evaluacionRepository.getEvaluaciones().collectLatest { evaluaciones ->
                val polinizadorEvaluaciones = evaluaciones.filter {
                    it.idPolinizador == idPolinizador && it.semana == week && it.evaluacionGeneralId == evaluacionGeneralId
                }
                val uniquePalmsCount = countUniquePalms(polinizadorEvaluaciones, evaluacionGeneralId)
                _totalPalmas.value = uniquePalmsCount
                Log.d("EvaluacionViewModel", "Total palmas únicas para polinizador $idPolinizador en semana $week y evaluacionGeneralId $evaluacionGeneralId: $uniquePalmsCount")
            }
        }
    }

    private fun setCurrentWeek() {
        val calendar = Calendar.getInstance()
        _currentWeek.value = calendar.get(Calendar.WEEK_OF_YEAR)
    }

    fun countUniquePalms(evaluaciones: List<EvaluacionPolinizacion>, evaluacionGeneralId: Int?): Int {
        return evaluaciones
            .filter { it.palma != null && it.evaluacionGeneralId == evaluacionGeneralId }
            .map { Pair(it.idlote, it.palma) }
            .distinct()
            .count()
    }
}