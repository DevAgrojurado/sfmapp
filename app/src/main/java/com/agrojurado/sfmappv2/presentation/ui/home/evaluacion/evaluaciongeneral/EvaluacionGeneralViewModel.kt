package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluaciongeneral

import android.content.Context
import android.util.Log
import android.widget.ProgressBar
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrojurado.sfmappv2.data.sync.DataSyncManager
import com.agrojurado.sfmappv2.domain.model.EvaluacionGeneral
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.domain.repository.EvaluacionGeneralRepository
import com.agrojurado.sfmappv2.domain.repository.EvaluacionPolinizacionRepository
import com.agrojurado.sfmappv2.domain.repository.LoteRepository
import com.agrojurado.sfmappv2.domain.repository.OperarioRepository
import com.agrojurado.sfmappv2.domain.repository.UsuarioRepository
import com.agrojurado.sfmappv2.domain.security.UserRoleConstants
import com.agrojurado.sfmappv2.utils.NetworkMonitor
import com.agrojurado.sfmappv2.data.remote.dto.common.utils.NetworkManager
import com.agrojurado.sfmappv2.data.sync.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import javax.inject.Inject
import com.agrojurado.sfmappv2.utils.SyncNotificationManager
import kotlinx.coroutines.flow.distinctUntilChanged

@HiltViewModel
class EvaluacionGeneralViewModel @Inject constructor(
    private val evaluacionPolinizacionRepository: EvaluacionPolinizacionRepository,
    private val evaluacionGeneralRepository: EvaluacionGeneralRepository,
    private val usuarioRepository: UsuarioRepository,
    private val operarioRepository: OperarioRepository,
    private val loteRepository: LoteRepository,
    private val networkMonitor: NetworkMonitor,
    private val dataSyncManager: DataSyncManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _evaluacionesIndividuales = MutableLiveData<List<EvaluacionPolinizacion>>()
    val evaluacionesIndividuales: LiveData<List<EvaluacionPolinizacion>> = _evaluacionesIndividuales

    private val _evaluacionGeneral = MutableLiveData<EvaluacionGeneral?>()
    val evaluacionGeneral: LiveData<EvaluacionGeneral?> = _evaluacionGeneral

    private val _temporaryEvaluacionId = MutableLiveData<Int?>()
    val temporaryEvaluacionId: LiveData<Int?> = _temporaryEvaluacionId

    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _syncStatus = MutableLiveData<SyncStatus>()
    val syncStatus: LiveData<SyncStatus> = _syncStatus

    private val _evaluacionesGeneralesPorSemana = MutableLiveData<Map<Int, List<EvaluacionGeneral>>>()
    val evaluacionesGeneralesPorSemana: LiveData<Map<Int, List<EvaluacionGeneral>>> = _evaluacionesGeneralesPorSemana

    private val _operarioMap = MutableLiveData<Map<Int, String>>()
    val operarioMap: LiveData<Map<Int, String>> = _operarioMap

    private val _loteMap = MutableLiveData<Map<Int, String>>()
    val loteMap: LiveData<Map<Int, String>> = _loteMap

    private val _evaluadorMap = MutableLiveData<Map<Int, String>>()
    val evaluadorMap: LiveData<Map<Int, String>> = _evaluadorMap

    private val _loggedInUser = MutableLiveData<Usuario?>()
    val loggedInUser: LiveData<Usuario?> = _loggedInUser

    private val _evaluacionesPolinizacion = MutableLiveData<List<EvaluacionPolinizacion>>()
    val evaluacionesPolinizacion: LiveData<List<EvaluacionPolinizacion>> get() = _evaluacionesPolinizacion

    private var cachedEvaluacionesGenerales: Map<Int, List<EvaluacionGeneral>>? = null
    private var cachedEvaluacionesPorPolinizador: MutableMap<Int, Map<Pair<Int, String>, List<EvaluacionPolinizacion>>> = mutableMapOf()
    private var cachedOperariosIds: Set<Int>? = null
    private var selectedPolinizadorId: Int = 0
    private var selectedLoteId: Int = 0
    private var lastSyncTime: Long = 0
    private val SYNC_THROTTLE_MS = 10_000L

    private val notificationManager = SyncNotificationManager.getInstance(context)

    init {
        loadLoggedInUser()
        loadOperarioMap()
        loadLoteMap()
        loadEvaluadorMap()
    }

    fun setSelectedPolinizadorId(id: Int) {
        selectedPolinizadorId = id
    }

    fun setSelectedLoteId(id: Int) {
        selectedLoteId = id
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

    fun loadEvaluacionesGeneralesPorSemana() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                cachedEvaluacionesGenerales?.let {
                    _evaluacionesGeneralesPorSemana.value = it
                    Log.d("EvaluacionGeneralVM", "Datos cargados desde caché: $it")
                    return@launch
                }
                evaluacionGeneralRepository.getAllEvaluacionesGenerales()
                    .collectLatest { evaluaciones ->
                        val filteredEvaluaciones = filterEvaluacionesByRole(evaluaciones)
                        val groupedBySemana = filteredEvaluaciones.filter { !it.isTemporary }
                            .groupBy { it.semana }
                        cachedEvaluacionesGenerales = groupedBySemana
                        _evaluacionesGeneralesPorSemana.value = groupedBySemana
                        Log.d("EvaluacionGeneralVM", "Datos cargados: $groupedBySemana")
                    }
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar evaluaciones generales: ${e.message}"
                Log.e("EvaluacionGeneralVM", "Error: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getPhotoUrlForPolinizador(semana: Int, polinizadorId: Int, evaluacionGeneralId: Int): String? {
        Log.d(TAG, "Buscando foto para semana=$semana, polinizadorId=$polinizadorId, evaluacionGeneralId=$evaluacionGeneralId")

        val evaluacionesGenerales = _evaluacionesGeneralesPorSemana.value?.get(semana)

        if (evaluacionesGenerales == null) {
            Log.d(TAG, "No hay evaluaciones en caché para la semana $semana")
            return null
        }

        val evaluacion = evaluacionesGenerales.firstOrNull {
            it.idpolinizadorev == polinizadorId && it.id == evaluacionGeneralId
        }

        if (evaluacion == null) {
            Log.d(TAG, "No se encontró evaluación para polinizador=$polinizadorId, evaluacionId=$evaluacionGeneralId")
            return null
        }

        Log.d(TAG, "Evaluación encontrada, fotoPath=${evaluacion.fotoPath}")

        if (evaluacion.fotoPath == null) {
            return null
        }

        val path = evaluacion.fotoPath

        return when {
            File(path).exists() -> {
                Log.d(TAG, "La foto existe localmente en: $path")
                path
            }
            path.startsWith("http") -> {
                Log.d(TAG, "Usando URL del servidor: $path")
                path
            }
            File(context.filesDir, File(path).name).exists() -> {
                val alternativePath = File(context.filesDir, File(path).name).absolutePath
                Log.d(TAG, "Foto encontrada en ubicación alternativa: $alternativePath")
                alternativePath
            }
            else -> {
                Log.e(TAG, "No se pudo encontrar la foto en: $path")
                null
            }
        }
    }

    private suspend fun filterEvaluacionesByRole(evaluaciones: List<EvaluacionGeneral>): List<EvaluacionGeneral> {
        val user = _loggedInUser.value ?: return emptyList()
        return when {
            user.rol.equals(UserRoleConstants.ROLE_ADMIN, ignoreCase = true) ||
                    user.rol.equals(UserRoleConstants.ROLE_COORDINATOR, ignoreCase = true) -> {
                evaluaciones
            }
            user.rol.equals(UserRoleConstants.ROLE_EVALUATOR, ignoreCase = true) -> {
                val operariosIds = cachedOperariosIds ?: run {
                    operarioRepository.getAllOperarios()
                        .firstOrNull()
                        ?.filter { it.fincaId == user.idFinca }
                        ?.map { it.id }
                        ?.toSet()
                        ?.also { cachedOperariosIds = it } ?: emptySet()
                }
                evaluaciones.filter { eval ->
                    operariosIds.contains(eval.idpolinizadorev) || eval.idevaluadorev == user.id
                }
            }
            else -> emptyList()
        }
    }

    suspend fun getEvaluacionesPorPolinizador(semana: Int): Map<Pair<Int, String>, List<EvaluacionPolinizacion>> {
        cachedEvaluacionesPorPolinizador[semana]?.let {
            Log.d("EvaluacionGeneralVM", "Datos desde caché para semana $semana: $it")
            return it
        }

        val evaluacionesGenerales = _evaluacionesGeneralesPorSemana.value?.get(semana) ?: emptyList()
        val allEvaluacionesPolinizacion = mutableListOf<EvaluacionPolinizacion>()

        for (evalGeneral in evaluacionesGenerales) {
            evalGeneral.id?.let { evalGeneralId ->
                val evaluacionesIndividuales = evaluacionPolinizacionRepository
                    .getEvaluacionesByEvaluacionGeneralId(evalGeneralId)
                    .firstOrNull() ?: emptyList()
                allEvaluacionesPolinizacion.addAll(evaluacionesIndividuales)
            }
        }

        val operarioMap = _operarioMap.value ?: emptyMap()
        val result = allEvaluacionesPolinizacion.groupBy {
            Pair(it.idPolinizador, operarioMap[it.idPolinizador] ?: "Desconocido")
        }.toSortedMap(compareBy { it.second })

        cachedEvaluacionesPorPolinizador[semana] = result.toMap()
        return result
    }

    fun clearCache() {
        cachedEvaluacionesGenerales = null
        cachedEvaluacionesPorPolinizador.clear()
        cachedOperariosIds = null
    }

    private fun loadOperarioMap() {
        viewModelScope.launch {
            operarioRepository.getAllOperarios().collectLatest { operarios ->
                _operarioMap.value = operarios.associate { it.id to "${it.codigo} ${it.nombre}" }
            }
        }
    }

    private fun loadLoteMap() {
        viewModelScope.launch {
            loteRepository.getAllLotes().collectLatest { lotes ->
                _loteMap.value = lotes.associate { it.id to (it.descripcion ?: "") }
            }
        }
    }

    private fun loadEvaluadorMap() {
        viewModelScope.launch {
            usuarioRepository.getAllUsersUseCase().collectLatest { usuarios ->
                _evaluadorMap.value = usuarios.associate { it.id to it.nombre }
            }
        }
    }

    fun setTemporaryEvaluacionId(id: Int) {
        _temporaryEvaluacionId.value = id
        Log.d(TAG, "TemporaryEvaluacionId establecido: $id")
    }

    fun initTemporaryEvaluacion() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "Inicializando nueva evaluación temporal")
                // Limpia explícitamente el estado y el caché
                resetState()
                clearCache()
                val user = _loggedInUser.value ?: run {
                    return@launch
                }
                val nuevaEvaluacion = EvaluacionGeneral(
                    serverId = null,
                    fecha = getCurrentDate(),
                    hora = getCurrentTime(),
                    semana = getCurrentWeek(),
                    idevaluadorev = user.id,
                    idpolinizadorev = null,
                    idLoteev = null,
                    syncStatus = "PENDING",
                    isTemporary = true,
                    timestamp = System.currentTimeMillis(),
                    fotoPath = null,
                    firmaPath = null
                )
                val tempId = evaluacionGeneralRepository.insertEvaluacionGeneral(nuevaEvaluacion).toInt()
                if (tempId <= 0) {
                    _errorMessage.value = "Fallo al crear evaluación temporal"
                    Log.e(TAG, "Fallo al crear evaluación temporal")
                    _isLoading.value = false
                    return@launch
                }
                val insertedEval = nuevaEvaluacion.copy(id = tempId)
                _temporaryEvaluacionId.value = tempId
                _evaluacionGeneral.value = insertedEval
                _evaluacionesIndividuales.value = emptyList()
                Log.d(TAG, "Evaluación temporal creada con ID: $tempId")
                // Forzar la recarga de evaluaciones individuales
                loadEvaluacionesIndividuales()
            } catch (e: Exception) {
                _errorMessage.value = "Error al inicializar evaluación: ${e.message}"
                Log.e(TAG, "Error al inicializar evaluación: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadEvaluacionesIndividuales() {
        viewModelScope.launch {
            val tempId = _temporaryEvaluacionId.value
            Log.d(TAG, "Cargando evaluaciones individuales para temporaryEvaluacionId: $tempId")
            if (tempId == null) {
                _evaluacionesIndividuales.value = emptyList()
                Log.w(TAG, "No hay temporaryEvaluacionId definido, lista vacía")
                return@launch
            }
            try {
                evaluacionPolinizacionRepository.getEvaluacionesByEvaluacionGeneralId(tempId)
                    .distinctUntilChanged() // Evita emisiones redundantes
                    .collect { evaluaciones ->
                        val filteredEvaluaciones = evaluaciones.filter { it.evaluacionGeneralId == tempId }
                        if (filteredEvaluaciones != _evaluacionesIndividuales.value) {
                            Log.d(TAG, "Evaluaciones individuales cargadas: $filteredEvaluaciones")
                            _evaluacionesIndividuales.value = filteredEvaluaciones
                        }
                    }
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar evaluaciones: ${e.message}"
                Log.e(TAG, "Error al cargar evaluaciones: ${e.message}", e)
                _evaluacionesIndividuales.value = emptyList()
            }
        }
    }

    fun hasEvaluacionesIndividuales(): Boolean {
        return _evaluacionesIndividuales.value?.isNotEmpty() ?: false
    }

    fun guardarEvaluacionGeneral(fotoPath: String?, firmaPath: String?, context: Context) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                if (selectedPolinizadorId <= 0 || selectedLoteId <= 0) {
                    _errorMessage.value = "Debe seleccionar un polinizador y lote válidos"
                    _saveResult.value = false
                    return@launch
                }
                val tempId = _temporaryEvaluacionId.value
                val tempEval = _evaluacionGeneral.value
                if (tempId == null || tempEval == null || tempEval.id == null) {
                    _errorMessage.value = "No se encontró una evaluación temporal activa"
                    _saveResult.value = false
                    return@launch
                }
                val finalFotoPath = fotoPath?.let { moveToPermanentFile(it, "foto_${tempEval.id}", context) }
                val finalFirmaPath = firmaPath?.let { moveToPermanentFile(it, "firma_${tempEval.id}", context) }
                val updatedEval = tempEval.copy(
                    idpolinizadorev = selectedPolinizadorId,
                    idLoteev = selectedLoteId,
                    isTemporary = false,
                    fotoPath = finalFotoPath,
                    firmaPath = finalFirmaPath,
                    syncStatus = "PENDING"
                )
                evaluacionGeneralRepository.updateEvaluacionGeneral(updatedEval)
                evaluacionGeneralRepository.finalizeTemporaryEvaluacion(tempEval.id!!)
                // Limpieza adicional
                resetState()
                clearCache()
                _evaluacionesIndividuales.value = emptyList()
                _saveResult.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Error al guardar: ${e.message}"
                _saveResult.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun moveToPermanentFile(tempPath: String, fileName: String, context: Context): String {
        val tempFile = java.io.File(tempPath)
        val permanentFile = java.io.File(context.filesDir, "$fileName.png")
        tempFile.copyTo(permanentFile, overwrite = true)
        tempFile.delete()
        return permanentFile.absolutePath
    }

    fun deleteEvaluacion(evaluacion: EvaluacionPolinizacion) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                evaluacionPolinizacionRepository.deleteEvaluacion(evaluacion)
                loadEvaluacionesIndividuales()
                clearCache()
            } catch (e: Exception) {
                _errorMessage.value = "Error al eliminar: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cleanUpTemporary() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val tempId = _temporaryEvaluacionId.value
                val tempEval = _evaluacionGeneral.value
                if (tempId != null && tempEval != null && tempEval.isTemporary == true) {
                    evaluacionPolinizacionRepository.deleteEvaluacionesByEvaluacionGeneralId(tempId)
                    evaluacionGeneralRepository.deleteEvaluacionGeneral(tempEval)
                    resetState()
                    clearCache()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al cancelar evaluación: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun resetState() {
        _temporaryEvaluacionId.value = null
        _evaluacionGeneral.value = null
        _evaluacionesIndividuales.value = emptyList()
        selectedPolinizadorId = 0
        selectedLoteId = 0
        _errorMessage.value = null
        _saveResult.value = false
        cachedEvaluacionesPorPolinizador.clear() // Limpia explícitamente el caché de evaluaciones por polinizador
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearSaveResult() {
        _saveResult.value = false
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val year = calendar.get(Calendar.YEAR)
        return "$day/$month/$year"
    }

    private fun getCurrentTime(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val minute = calendar.get(Calendar.MINUTE).toString().padStart(2, '0')
        val second = calendar.get(Calendar.SECOND).toString().padStart(2, '0')
        return "$hour:$minute:$second"
    }

    private fun getCurrentWeek(): Int {
        return Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)
    }

    override fun onCleared() {
        super.onCleared()
        clearCache()
    }

    fun forceSync() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _syncStatus.value = SyncStatus.Syncing

                val pendingCount = evaluacionGeneralRepository.getUnsyncedEvaluationsCount()
                if (pendingCount > 0) {
                    _errorMessage.value = "Sincronizando $pendingCount evaluaciones pendientes..."
                    notificationManager.startSyncNotification(
                        "Sincronización manual",
                        "Sincronizando $pendingCount evaluaciones pendientes..."
                    )
                } else {
                    _errorMessage.value = "No hay evaluaciones pendientes, verificando datos del servidor..."
                    notificationManager.startSyncNotification(
                        "Sincronización manual",
                        "Verificando datos con el servidor..."
                    )
                }

                if (!NetworkManager.isNetworkAvailable(context)) {
                    val unsyncedCount = evaluacionGeneralRepository.getUnsyncedEvaluationsCount()
                    _syncStatus.value = if (unsyncedCount > 0) SyncStatus.Pending(unsyncedCount) else SyncStatus.Completed
                    _errorMessage.value = "Sin conexión, $unsyncedCount evaluaciones pendientes"
                    notificationManager.updateSyncMessage("Sin conexión, $unsyncedCount evaluaciones pendientes")
                    return@launch
                }

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastSyncTime < SYNC_THROTTLE_MS) {
                    val unsyncedCount = evaluacionGeneralRepository.getUnsyncedEvaluationsCount()
                    _syncStatus.value = SyncStatus.Pending(unsyncedCount)
                    _errorMessage.value = "Espere un momento antes de intentar sincronizar de nuevo"
                    notificationManager.updateSyncMessage("Espere un momento antes de intentar sincronizar de nuevo")
                    return@launch
                }

                // Usar DataSyncManager para sincronizar
                val dummyProgressBar = ProgressBar(context).apply { visibility = ProgressBar.GONE }
                dataSyncManager.syncAllData(dummyProgressBar) {
                    viewModelScope.launch {
                        val remainingCount = evaluacionGeneralRepository.getUnsyncedEvaluationsCount()
                        _syncStatus.value = if (remainingCount > 0) {
                            SyncStatus.Pending(remainingCount)
                        } else {
                            SyncStatus.Completed
                        }

                        val message = if (remainingCount > 0) {
                            "Quedan $remainingCount evaluaciones pendientes de sincronización. Reintente manualmente."
                        } else {
                            "Sincronización de evaluaciones completada"
                        }

                        _errorMessage.value = message
                        if (remainingCount > 0) {
                            notificationManager.updateSyncMessage(message)
                        } else {
                            notificationManager.completeSyncNotification(message)
                        }

                        Log.d(TAG, "✅ Sincronización forzada completada: $message")
                        clearCache()
                        lastSyncTime = System.currentTimeMillis()
                    }
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error(e.message ?: "Error de sincronización")
                _errorMessage.value = "Error durante la sincronización: ${e.message}"
                notificationManager.errorSyncNotification("Error: ${e.message}")
                Log.e(TAG, "❌ Error al forzar sincronización: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        private const val TAG = "EvaluacionGeneralVM"
    }
}