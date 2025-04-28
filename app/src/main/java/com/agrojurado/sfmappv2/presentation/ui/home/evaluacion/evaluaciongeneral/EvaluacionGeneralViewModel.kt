package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluaciongeneral

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrojurado.sfmappv2.data.sync.SyncStatus
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
import com.agrojurado.sfmappv2.data.remote.dto.common.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.agrojurado.sfmappv2.utils.SyncNotificationManager

@HiltViewModel
class EvaluacionGeneralViewModel @Inject constructor(
    private val evaluacionPolinizacionRepository: EvaluacionPolinizacionRepository,
    private val evaluacionGeneralRepository: EvaluacionGeneralRepository,
    private val usuarioRepository: UsuarioRepository,
    private val operarioRepository: OperarioRepository,
    private val loteRepository: LoteRepository,
    private val networkMonitor: NetworkMonitor,
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
        networkMonitor.observeForever { isConnected ->
            if (isConnected && _temporaryEvaluacionId.value == null) {
                viewModelScope.launch {
                    try {
                        val unsyncedCount = evaluacionGeneralRepository.getUnsyncedEvaluationsCount()
                        if (unsyncedCount > 0) {
                            Log.d(TAG, "Conexión detectada con $unsyncedCount evaluaciones pendientes y sin evaluación temporal activa. Iniciando sincronización automática.")
                            syncEvaluaciones()
                        } else {
                            Log.d(TAG, "Conexión detectada, pero no hay evaluaciones pendientes o hay una evaluación temporal activa.")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al verificar evaluaciones pendientes para sincronización automática: ${e.message}", e)
                    }
                }
            } else if (isConnected) {
                Log.d(TAG, "Conexión detectada, pero hay una evaluación temporal activa. Sincronización automática omitida.")
            }
        }
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
                        loadEvaluacionesGeneralesPorSemana()
                    }
                } ?: run {
                    _errorMessage.value = "No se encontró usuario logueado"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar usuario: ${e.message}"
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
        
        // Primero intentamos obtener las evaluaciones de caché
        val evaluacionesGenerales = _evaluacionesGeneralesPorSemana.value?.get(semana)
        
        if (evaluacionesGenerales == null) {
            Log.d(TAG, "No hay evaluaciones en caché para la semana $semana")
            return null
        }
        
        // Buscamos la evaluación específica
        val evaluacion = evaluacionesGenerales.firstOrNull {
            it.idpolinizadorev == polinizadorId && it.id == evaluacionGeneralId
        }
        
        if (evaluacion == null) {
            Log.d(TAG, "No se encontró evaluación para polinizador=$polinizadorId, evaluacionId=$evaluacionGeneralId")
            return null
        }
        
        Log.d(TAG, "Evaluación encontrada, fotoPath=${evaluacion.fotoPath}")
        
        // Si el path es nulo, no hay foto
        if (evaluacion.fotoPath == null) {
            return null
        }
        
        // Verificamos el path
        val path = evaluacion.fotoPath
        
        return when {
            File(path).exists() -> {
                Log.d(TAG, "La foto existe localmente en: $path")
                path // Usa el archivo local si existe
            }
            path.startsWith("http") -> {
                Log.d(TAG, "Usando URL del servidor: $path")
                path // Usa la URL del servidor si es válida
            }
            // Intentamos buscar la foto en otras ubicaciones comunes
            File(context.filesDir, File(path).name).exists() -> {
                val alternativePath = File(context.filesDir, File(path).name).absolutePath
                Log.d(TAG, "Foto encontrada en ubicación alternativa: $alternativePath")
                alternativePath
            }
            else -> {
                Log.e(TAG, "No se pudo encontrar la foto en: $path")
                null // No hay un path válido
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

    fun initTemporaryEvaluacion() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val activeEvaluacion = evaluacionGeneralRepository.getActiveTemporaryEvaluacion()
                activeEvaluacion?.let {
                    if (it.isTemporary) {
                        evaluacionPolinizacionRepository.deleteEvaluacionesByEvaluacionGeneralId(it.id!!)
                        evaluacionGeneralRepository.deleteEvaluacionGeneral(it)
                    }
                }

                val user = _loggedInUser.value ?: run {
                    _errorMessage.value = "No hay usuario logueado"
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
                    isSynced = false,
                    isTemporary = true,
                    timestamp = System.currentTimeMillis(),
                    fotoPath = null,
                    firmaPath = null
                )

                val tempId = evaluacionGeneralRepository.insertEvaluacionGeneral(nuevaEvaluacion).toInt()
                if (tempId <= 0) {
                    _errorMessage.value = "Fallo al crear evaluación temporal"
                    return@launch
                }

                val insertedEval = nuevaEvaluacion.copy(id = tempId)
                _temporaryEvaluacionId.value = tempId
                _evaluacionGeneral.value = insertedEval
                _evaluacionesIndividuales.value = emptyList()
            } catch (e: Exception) {
                _errorMessage.value = "Error al inicializar evaluación: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadEvaluacionesIndividuales() {
        viewModelScope.launch {
            val tempId = _temporaryEvaluacionId.value ?: run {
                _evaluacionesIndividuales.value = emptyList()
                return@launch
            }
            evaluacionPolinizacionRepository.getEvaluacionesByEvaluacionGeneralId(tempId)
                .collectLatest { evaluaciones ->
                    _evaluacionesIndividuales.value = evaluaciones
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
                    isSynced = false
                )
                evaluacionGeneralRepository.updateEvaluacionGeneral(updatedEval)
                evaluacionGeneralRepository.finalizeTemporaryEvaluacion(tempEval.id!!)

                // Trigger synchronization after saving
                syncEvaluaciones()

                resetState()
                clearCache()
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

    private fun syncEvaluaciones() {
        viewModelScope.launch {
            // --- Comprobación de Red Primero --- 
            if (!Utils.isNetworkAvailable(context)) {
                Log.d(TAG, "syncEvaluaciones: No hay conexión validada. Actualizando estado pendiente.")
                try {
                    val unsyncedCount = evaluacionGeneralRepository.getUnsyncedEvaluationsCount()
                    _syncStatus.value = if (unsyncedCount > 0) SyncStatus.Pending(unsyncedCount) else SyncStatus.Completed
                    if (unsyncedCount > 0) {
                         // Opcional: Mostrar un Toast indicando guardado local y pendiente
                         // Toast.makeText(context, "Guardado localmente, sincronización pendiente", Toast.LENGTH_SHORT).show()
                    } else {
                        // Toast.makeText(context, "Guardado localmente", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al obtener contador de pendientes offline: ${e.message}", e)
                     _syncStatus.value = SyncStatus.Error("Error al verificar pendientes offline")
                }
                return@launch // No continuar con el intento de sincronización
            }
            // --- Fin Comprobación de Red ---

            // Si hay conexión, proceder como antes...
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastSyncTime < SYNC_THROTTLE_MS) {
                val unsyncedCount = evaluacionGeneralRepository.getUnsyncedEvaluationsCount()
                _syncStatus.value = SyncStatus.Pending(unsyncedCount)
                Log.d(TAG, "syncEvaluaciones: Throttled, actualizando estado pendiente.")
                return@launch
            }

            try {
                _isLoading.value = true
                _syncStatus.value = SyncStatus.Syncing // Ahora sí mostramos "Sincronizando..."
                
                // Iniciar notificación (solo si estamos online)
                val unsyncedCount = evaluacionGeneralRepository.getUnsyncedEvaluationsCount()
                if (unsyncedCount > 0) {
                    notificationManager.startSyncNotification(
                        "Sincronizando evaluaciones", 
                        "Preparando sincronización de $unsyncedCount evaluaciones..."
                    )
                } else {
                    // Si no hay pendientes pero hay conexión, podemos verificar datos del servidor
                    notificationManager.startSyncNotification(
                        "Sincronización", 
                        "Verificando datos con el servidor..."
                    )
                }
                
                // Mostrar progreso indeterminado mientras se sincroniza (solo si estamos online)
                notificationManager.updateSyncProgress(0, 1, "Sincronizando evaluaciones...")
                
                // Llamar al repositorio (que también verifica la red internamente por si acaso)
                evaluacionGeneralRepository.syncEvaluacionesGenerales()
                lastSyncTime = System.currentTimeMillis()
                clearCache()
                
                // Actualizar estado y notificación final
                val pendingCount = evaluacionGeneralRepository.getUnsyncedEvaluationsCount()
                _syncStatus.value = if (pendingCount > 0) SyncStatus.Pending(pendingCount) else SyncStatus.Completed
                
                if (pendingCount > 0) {
                    notificationManager.updateSyncMessage("Quedan $pendingCount evaluaciones pendientes")
                } else {
                    notificationManager.completeSyncNotification("Sincronización completada")
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error(e.message ?: "Sync failed")
                _errorMessage.value = "Error durante la sincronización: ${e.message}"
                notificationManager.errorSyncNotification("Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
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

    // Método para forzar la sincronización desde cualquier parte de la app
    fun forceSync() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _syncStatus.value = SyncStatus.Syncing
                
                // Obtener cuántas evaluaciones hay pendientes
                val pendingCount = evaluacionGeneralRepository.getUnsyncedEvaluationsCount()
                
                // Iniciar notificación con barra de progreso
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

                // Mostrar progreso
                notificationManager.updateSyncProgress(0, 100, "Iniciando sincronización...")

                // Realizar la sincronización
                val result = evaluacionGeneralRepository.syncEvaluacionesGenerales()
                lastSyncTime = System.currentTimeMillis()

                // Actualizar progreso
                notificationManager.updateSyncProgress(50, 100, "Procesando resultados...")

                // Sincronizar explícitamente las evaluaciones de polinización
                try {
                    notificationManager.updateSyncProgress(75, 100, "Sincronizando evaluaciones de polinización...")
                    evaluacionPolinizacionRepository.fetchEvaluacionesFromServer()
                    Log.d(TAG, "✅ Sincronización explícita de evaluaciones de polinización completada")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error en sincronización explícita de polinización: ${e.message}", e)
                    // Continuamos a pesar del error para no bloquear el resto del proceso
                }

                // Limpiar caché para asegurar datos actualizados
                clearCache()

                // Finalizar progreso
                notificationManager.updateSyncProgress(100, 100, "Finalizando sincronización...")

                // Verificar si quedaron evaluaciones pendientes
                val remainingCount = evaluacionGeneralRepository.getUnsyncedEvaluationsCount()
                _syncStatus.value = if (remainingCount > 0) {
                    SyncStatus.Pending(remainingCount)
                } else {
                    SyncStatus.Completed
                }

                // Mostrar mensaje apropiado
                val message = if (remainingCount > 0) {
                    "Quedan $remainingCount evaluaciones pendientes de sincronización"
                } else {
                    "Sincronización de evaluaciones completada"
                }

                _errorMessage.value = message

                // Actualizar notificación final
                if (remainingCount > 0) {
                    notificationManager.updateSyncMessage(message)
                } else {
                    notificationManager.completeSyncNotification(message)
                }

                Log.d(TAG, "Sincronización forzada completada: $message")
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error(e.message ?: "Error de sincronización")
                _errorMessage.value = "Error durante la sincronización: ${e.message}"
                notificationManager.errorSyncNotification("Error: ${e.message}")
                Log.e(TAG, "Error al forzar sincronización: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        private const val TAG = "EvaluacionGeneralVM"
    }
}