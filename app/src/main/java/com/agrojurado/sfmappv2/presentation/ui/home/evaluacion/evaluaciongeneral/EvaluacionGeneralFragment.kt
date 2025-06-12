package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluaciongeneral

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.data.sync.SyncStatus
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluacionfragmentsform.EvaluacionActivity
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.shared.SharedSelectionViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EvaluacionGeneralFragment : Fragment() {
    private val viewModel: EvaluacionGeneralViewModel by activityViewModels()
    private val sharedViewModel: SharedSelectionViewModel by activityViewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var evaluacionesAdapter: EvaluacionesListAdapter
    private lateinit var btnGuardarGeneral: Button
    private lateinit var btnCancelar: Button
    private lateinit var fabAddEvaluacion: FloatingActionButton
    private lateinit var tvNoEvaluaciones: TextView
    private lateinit var tvTotalEvaluaciones: TextView

    // Cambiado de Spinner a AutoCompleteTextView
    private lateinit var autoCompletePolinizador: AutoCompleteTextView
    private lateinit var autoCompleteLote: AutoCompleteTextView

    private lateinit var etSeccion: EditText
    private lateinit var ivFoto: ImageView
    private lateinit var btnAddFoto: Button
    private lateinit var signatureContainer: LinearLayout
    private lateinit var ivSignature: ImageView
    private lateinit var tvTapToSign: TextView
    private lateinit var btnClearFirma: Button

    private var isUpdatingFromSharedViewModel = false
    private var fotoPath: String? = null
    private var firmaPath: String? = null

    // Adaptadores para AutoCompleteTextView
    private var polinizadorAdapter: ArrayAdapter<String>? = null
    private var loteAdapter: ArrayAdapter<String>? = null

    // Lanzador para iniciar EvaluacionActivity y recibir el resultado
    private val startEvaluacionActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data?.getBooleanExtra("SAVE_SUCCESS", false) == true) {
                // La evaluación se guardó correctamente, recargar las evaluaciones individuales
                viewModel.loadEvaluacionesIndividuales()
                //Toast.makeText(requireContext(), "Evaluación individual agregada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        const val REQUEST_KEY_PHOTO = "request_photo"
        const val BUNDLE_KEY_PHOTO = "photo_path"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_evaluacion_general, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRecyclerView()
        setHasOptionsMenu(true)

        // Restaurar estado si existe
        savedInstanceState?.let {
            val restoredTempId = it.getInt("temporaryEvaluacionId", -1)
            if (restoredTempId != -1 && viewModel.temporaryEvaluacionId.value == null) {
                viewModel.setTemporaryEvaluacionId(restoredTempId)
                Log.d("EvaluacionGeneralFragment", "Restored temporaryEvaluacionId: $restoredTempId")
            }
        }

        // Observar el usuario logueado
        viewModel.loggedInUser.observe(viewLifecycleOwner) { user ->
            if (user == null) {
                Log.w("EvaluacionGeneralFragment", "Usuario no cargado, esperando...")
                Toast.makeText(requireContext(), "Cargando usuario, por favor espera...", Toast.LENGTH_SHORT).show()
            } else {
                // Solo inicializar si no hay un ID temporal activo
                if (viewModel.temporaryEvaluacionId.value == null) {
                    viewModel.initTemporaryEvaluacion()
                    Log.d("EvaluacionGeneralFragment", "Initialized new temporary evaluation")
                }
            }
        }

        sharedViewModel.loadOperarios()
        sharedViewModel.loadLotes()
        setupObservers()
        setupListeners()

        // Verificar estado inicial de los widgets
        viewModel.evaluacionesIndividuales.value?.let { evaluaciones ->
            updateEvaluacionesList(evaluaciones)
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isAdded) {
                    handleCancelAction()
                } else {
                    Log.w("EvaluacionGeneralFragment", "Fragment not attached, ignoring back press")
                }
            }
        })

        // Restaurar rutas desde savedInstanceState si existen
        savedInstanceState?.let {
            fotoPath = it.getString("fotoPath")
            firmaPath = it.getString("firmaPath")
            fotoPath?.let { path -> ivFoto.setImageBitmap(BitmapFactory.decodeFile(path)) }
            firmaPath?.let { path ->
                ivSignature.setImageBitmap(BitmapFactory.decodeFile(path))
                ivSignature.visibility = View.VISIBLE
                tvTapToSign.visibility = View.GONE
                btnClearFirma.visibility = View.VISIBLE
            }
        }

        setFragmentResultListener(REQUEST_KEY_PHOTO) { _, bundle ->
            val photoPath = bundle.getString(BUNDLE_KEY_PHOTO)
            if (photoPath != null && java.io.File(photoPath).exists()) {
                this.fotoPath = photoPath
                ivFoto.setImageBitmap(BitmapFactory.decodeFile(photoPath))
                Log.d("EvaluacionGeneralFragment", "Photo loaded from: $photoPath")
            } else {
                Log.e("EvaluacionGeneralFragment", "Failed to load photo from path: $photoPath")
                Toast.makeText(requireContext(), "No se pudo obtener la foto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                handleCancelAction()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // Ocultar el botón de sincronización
        menu.findItem(R.id.action_sync)?.isVisible = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("fotoPath", fotoPath)
        outState.putString("firmaPath", firmaPath)
        viewModel.temporaryEvaluacionId.value?.let { outState.putInt("temporaryEvaluacionId", it) }
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewEvaluaciones)
        btnGuardarGeneral = view.findViewById(R.id.btnGuardarGeneral)
        btnCancelar = view.findViewById(R.id.btnCancelar)
        fabAddEvaluacion = view.findViewById(R.id.fabAddEvaluacion)
        tvNoEvaluaciones = view.findViewById(R.id.tvNoEvaluaciones)
        tvTotalEvaluaciones = view.findViewById(R.id.tvTotalEvaluaciones)

        // Inicializar AutoCompleteTextView en lugar de Spinner
        autoCompletePolinizador = view.findViewById(R.id.spinnerPolinizador)
        autoCompleteLote = view.findViewById(R.id.spinnerLote)

        etSeccion = view.findViewById(R.id.etSeccion)
        ivFoto = view.findViewById(R.id.ivFoto)
        btnAddFoto = view.findViewById(R.id.btnAddFoto)
        signatureContainer = view.findViewById(R.id.signatureContainer)
        ivSignature = view.findViewById(R.id.ivSignature)
        tvTapToSign = view.findViewById(R.id.tvTapToSign)
        btnClearFirma = view.findViewById(R.id.btnClearFirma)
    }

    private fun setupRecyclerView() {
        evaluacionesAdapter = EvaluacionesListAdapter(
            onItemClickListener = { evaluacion -> showEvaluacionDetails(evaluacion) },
            onDeleteClickListener = { evaluacion -> viewModel.deleteEvaluacion(evaluacion) }
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = evaluacionesAdapter
        }
    }

    private fun setupObservers() {
        // Observer para operarios - actualizado para AutoCompleteTextView
        sharedViewModel.operarios.observe(viewLifecycleOwner) { operariosList ->
            if (operariosList.isNotEmpty()) {
                val operariosNames = operariosList.map { it.first }
                polinizadorAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    operariosNames
                )
                autoCompletePolinizador.setAdapter(polinizadorAdapter)

                // Configurar filtrado automático
                autoCompletePolinizador.threshold = 1 // Mostrar sugerencias desde el primer carácter

                // Establecer valor seleccionado si existe
                sharedViewModel.selectedOperarioId.value?.let { id ->
                    val selectedOperario = operariosList.find { it.second.id == id }
                    selectedOperario?.let {
                        isUpdatingFromSharedViewModel = true
                        autoCompletePolinizador.setText(it.first, false)
                        isUpdatingFromSharedViewModel = false
                    }
                }
            }
        }

        sharedViewModel.selectedOperarioId.observe(viewLifecycleOwner) { id ->
            id?.let {
                val operariosList = sharedViewModel.operarios.value ?: return@let
                val selectedOperario = operariosList.find { pair -> pair.second.id == it }
                selectedOperario?.let { operario ->
                    if (autoCompletePolinizador.text.toString() != operario.first) {
                        isUpdatingFromSharedViewModel = true
                        autoCompletePolinizador.setText(operario.first, false)
                        isUpdatingFromSharedViewModel = false
                    }
                }
            }
        }

        // Observer para lotes - actualizado para AutoCompleteTextView
        sharedViewModel.lotes.observe(viewLifecycleOwner) { lotesList ->
            if (lotesList.isNotEmpty()) {
                val lotesNames = lotesList.map { it.first }
                loteAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    lotesNames
                )
                autoCompleteLote.setAdapter(loteAdapter)

                // Configurar filtrado automático
                autoCompleteLote.threshold = 1 // Mostrar sugerencias desde el primer carácter

                // Establecer valor seleccionado si existe
                sharedViewModel.selectedLoteId.value?.let { id ->
                    val selectedLote = lotesList.find { it.second.id == id }
                    selectedLote?.let {
                        isUpdatingFromSharedViewModel = true
                        autoCompleteLote.setText(it.first, false)
                        isUpdatingFromSharedViewModel = false
                    }
                }
            }
        }

        sharedViewModel.selectedLoteId.observe(viewLifecycleOwner) { id ->
            id?.let {
                val lotesList = sharedViewModel.lotes.value ?: return@let
                val selectedLote = lotesList.find { pair -> pair.second.id == it }
                selectedLote?.let { lote ->
                    if (autoCompleteLote.text.toString() != lote.first) {
                        isUpdatingFromSharedViewModel = true
                        autoCompleteLote.setText(lote.first, false)
                        isUpdatingFromSharedViewModel = false
                    }
                }
            }
        }

        sharedViewModel.selectedSeccion.observe(viewLifecycleOwner) { seccion ->
            val currentSeccion = etSeccion.text.toString().toIntOrNull()
            if (seccion != null && seccion != currentSeccion) {
                isUpdatingFromSharedViewModel = true
                etSeccion.setText(seccion.toString())
                isUpdatingFromSharedViewModel = false
            }
        }

        viewModel.evaluacionesIndividuales.observe(viewLifecycleOwner) { evaluaciones ->
            updateEvaluacionesList(evaluaciones)
        }

        viewModel.saveResult.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                Toast.makeText(requireContext(), "Evaluación General guardada con éxito", Toast.LENGTH_SHORT).show()
                sharedViewModel.clearSelections()
                viewModel.clearSaveResult()
                findNavController().popBackStack(R.id.listaEvaluacionFragment, false)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.temporaryEvaluacionId.observe(viewLifecycleOwner) { tempId ->
            Log.d("EvaluacionGeneralFragment", "Temporary ID actual: $tempId")
        }

        viewModel.syncStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is SyncStatus.Syncing -> Toast.makeText(requireContext(), "Sincronizando...", Toast.LENGTH_SHORT).show()
                is SyncStatus.Completed -> Toast.makeText(requireContext(), "Sincronización completada", Toast.LENGTH_SHORT).show()
                is SyncStatus.Pending -> Toast.makeText(requireContext(), "${status.unsyncedCount} evaluaciones pendientes", Toast.LENGTH_SHORT).show()
                is SyncStatus.Error -> Toast.makeText(requireContext(), "Error al sincronizar: ${status.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupListeners() {
        fabAddEvaluacion.setOnClickListener {
            // Obtener IDs basados en el texto seleccionado en AutoCompleteTextView
            val selectedOperarioId = getSelectedOperarioId()
            val selectedLoteId = getSelectedLoteId()
            val selectedSeccion = etSeccion.text.toString().toIntOrNull()

            // Validar que el polinizador y el lote sean válidos
            if (selectedOperarioId == null) {
                Toast.makeText(requireContext(), "Seleccione un polinizador válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedLoteId == null) {
                Toast.makeText(requireContext(), "Seleccione un lote válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Opcional: Validar que la sección no esté vacía y sea un número válido
            if (selectedSeccion == null) {
                Toast.makeText(requireContext(), "Ingrese una sección válida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("EvaluacionGeneralFragment", "Iniciando EvaluacionActivity con Operario=$selectedOperarioId, Lote=$selectedLoteId, Seccion=$selectedSeccion")

            if (checkCameraPermission()) {
                val intent = Intent(requireContext(), EvaluacionActivity::class.java).apply {
                    putExtra("evaluacionGeneralId", viewModel.temporaryEvaluacionId.value ?: -1)
                    putExtra("operarioId", selectedOperarioId)
                    putExtra("loteId", selectedLoteId)
                    putExtra("seccion", selectedSeccion)
                }
                startEvaluacionActivity.launch(intent)
            } else {
                requestCameraPermission()
            }
        }

        btnGuardarGeneral.setOnClickListener {
            guardarEvaluacion()
        }

        btnCancelar.setOnClickListener {
            handleCancelAction()
        }

        // Listener para AutoCompleteTextView de Polinizador
        autoCompletePolinizador.setOnItemClickListener { _, _, position, _ ->
            if (!isUpdatingFromSharedViewModel) {
                val adapter = autoCompletePolinizador.adapter as? ArrayAdapter<String>
                val selectedText = adapter?.getItem(position)
                selectedText?.let { text ->
                    val operariosList = sharedViewModel.operarios.value
                    val operario = operariosList?.find { it.first == text }
                    operario?.let {
                        sharedViewModel.setSelectedOperarioId(it.second.id)
                        viewModel.setSelectedPolinizadorId(it.second.id)
                    }
                }
            }
        }

        // También escuchar cambios de texto para validación manual
        autoCompletePolinizador.doAfterTextChanged { text ->
            if (!isUpdatingFromSharedViewModel) {
                val inputText = text.toString()
                val operariosList = sharedViewModel.operarios.value
                val exactMatch = operariosList?.find { it.first.equals(inputText, ignoreCase = true) }

                if (exactMatch != null) {
                    // Coincidencia exacta encontrada
                    sharedViewModel.setSelectedOperarioId(exactMatch.second.id)
                    viewModel.setSelectedPolinizadorId(exactMatch.second.id)
                } else {
                    // No hay coincidencia exacta, limpiar selección
                    sharedViewModel.setSelectedOperarioId(null)
                    viewModel.setSelectedPolinizadorId(0)
                }
            }
        }

// Listener para AutoCompleteTextView de Lote
        autoCompleteLote.setOnItemClickListener { _, _, position, _ ->
            if (!isUpdatingFromSharedViewModel) {
                val adapter = autoCompleteLote.adapter as? ArrayAdapter<String>
                val selectedText = adapter?.getItem(position)
                selectedText?.let { text ->
                    val lotesList = sharedViewModel.lotes.value
                    val lote = lotesList?.find { it.first == text }
                    lote?.let {
                        sharedViewModel.setSelectedLoteId(it.second.id)
                        viewModel.setSelectedLoteId(it.second.id)
                    }
                }
            }
        }

        // Listener para cambios de texto en Lote
        autoCompleteLote.doAfterTextChanged { text ->
            if (!isUpdatingFromSharedViewModel) {
                val inputText = text.toString()
                val lotesList = sharedViewModel.lotes.value
                val exactMatch = lotesList?.find { it.first.equals(inputText, ignoreCase = true) }

                if (exactMatch != null) {
                    // Coincidencia exacta encontrada
                    sharedViewModel.setSelectedLoteId(exactMatch.second.id)
                    viewModel.setSelectedLoteId(exactMatch.second.id)
                } else {
                    // No hay coincidencia exacta, limpiar selección
                    sharedViewModel.setSelectedLoteId(null)
                    viewModel.setSelectedLoteId(0)
                }
            }
        }

        // Agregar listener para mostrar dropdown al hacer focus
        autoCompletePolinizador.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && polinizadorAdapter != null) {
                autoCompletePolinizador.showDropDown()
            }
        }

        autoCompleteLote.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && loteAdapter != null) {
                autoCompleteLote.showDropDown()
            }
        }


        etSeccion.doAfterTextChanged { text ->
            if (!isUpdatingFromSharedViewModel) {
                val seccion = text.toString().toIntOrNull()
                sharedViewModel.setSelectedSeccion(seccion)
            }
        }

        btnAddFoto.setOnClickListener {
            if (checkCameraPermission()) {
                takePhoto()
            } else {
                requestCameraPermission()
            }
        }

        signatureContainer.setOnClickListener {
            openSignatureBottomSheet()
        }

        btnClearFirma.setOnClickListener {
            ivSignature.visibility = View.GONE
            tvTapToSign.visibility = View.VISIBLE
            firmaPath = null
        }
    }

    // Función auxiliar para obtener ID del operario seleccionado
    private fun getSelectedOperarioId(): Int? {
        val selectedText = autoCompletePolinizador.text.toString()
        return sharedViewModel.operarios.value?.find { it.first == selectedText }?.second?.id
    }

    // Función auxiliar para obtener ID del lote seleccionado
    private fun getSelectedLoteId(): Int? {
        val selectedText = autoCompleteLote.text.toString()
        return sharedViewModel.lotes.value?.find { it.first == selectedText }?.second?.id
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
    }

    private fun isCameraAvailable(): Boolean {
        return requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (isCameraAvailable()) findNavController().navigate(R.id.action_evaluacionGeneralFragment_to_cameraFragment)
            else Toast.makeText(requireContext(), "No hay cámara disponible", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleCancelAction() {
        val tempId = viewModel.temporaryEvaluacionId.value
        if (tempId != null && viewModel.hasEvaluacionesIndividuales()) {
            AlertDialog.Builder(requireContext())
                .setTitle("⚠\uFE0F Cancelar Evaluación")
                .setMessage("Hay eventos pendientes. ¿Desea cancelar y perder estos registros?")
                .setPositiveButton("Sí") { _, _ ->
                    if (isAdded) {
                        viewModel.cleanUpTemporary()
                        Toast.makeText(requireContext(), "Evaluación cancelada", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack(R.id.listaEvaluacionFragment, false)
                    } else {
                        Log.w("EvaluacionGeneralFragment", "Fragment not attached, skipping navigation")
                    }
                }
                .setNegativeButton("No", null)
                .show()
        } else {
            if (isAdded) {
                viewModel.cleanUpTemporary()
                sharedViewModel.clearSelections()
                findNavController().popBackStack(R.id.listaEvaluacionFragment, false)
            } else {
                Log.w("EvaluacionGeneralFragment", "Fragment not attached, skipping navigation")
            }
        }
    }

    private fun updateEvaluacionesList(evaluaciones: List<EvaluacionPolinizacion>) {
        val tempId = viewModel.temporaryEvaluacionId.value
        Log.d("EvaluacionGeneralFragment", "Actualizando RecyclerView con evaluaciones: $evaluaciones, temporaryEvaluacionId: $tempId")
        if (tempId == null) {
            evaluacionesAdapter.submitList(emptyList())
            tvTotalEvaluaciones.text = "Total: 0 eventos"
            tvNoEvaluaciones.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            btnGuardarGeneral.isEnabled = false
            return
        }
        val filteredEvaluaciones = evaluaciones.filter { it.evaluacionGeneralId == tempId }
        recyclerView.post {
            evaluacionesAdapter.submitList(filteredEvaluaciones)
            tvTotalEvaluaciones.text = "Total: ${filteredEvaluaciones.size} eventos"
            tvNoEvaluaciones.visibility = if (filteredEvaluaciones.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (filteredEvaluaciones.isEmpty()) View.GONE else View.VISIBLE
            btnGuardarGeneral.isEnabled = filteredEvaluaciones.isNotEmpty()
            btnCancelar.isEnabled = true

            // Deshabilitar widgets si hay evaluaciones individuales
            val hasEvaluaciones = filteredEvaluaciones.isNotEmpty()
            autoCompletePolinizador.isEnabled = !hasEvaluaciones
            autoCompleteLote.isEnabled = !hasEvaluaciones
            etSeccion.isEnabled = !hasEvaluaciones
        }
    }

    private fun showEvaluacionDetails(evaluacion: EvaluacionPolinizacion) {
        Toast.makeText(requireContext(), "Lote: ${evaluacion.idlote}, Palma: ${evaluacion.palma ?: "N/A"}, Sección: ${evaluacion.seccion}", Toast.LENGTH_SHORT).show()
    }

/*    override fun onResume() {
        super.onResume()
    }*/

    private fun guardarEvaluacion() {
        if (viewModel.hasEvaluacionesIndividuales()) {
            if (firmaPath == null || fotoPath == null) {
                Toast.makeText(requireContext(), "Por favor, agregue una firma y foto antes de guardar", Toast.LENGTH_SHORT).show()
                return
            }
            viewModel.guardarEvaluacionGeneral(fotoPath, firmaPath, requireContext())
            fotoPath = null
            firmaPath = null
            ivFoto.setImageDrawable(null)
            ivSignature.setImageDrawable(null)
            ivSignature.visibility = View.GONE
            tvTapToSign.visibility = View.VISIBLE
            btnClearFirma.visibility = View.GONE
            sharedViewModel.clearSelections() // Limpia las selecciones después de guardar
        } else {
            Toast.makeText(requireContext(), "Agregue al menos una evaluación individual", Toast.LENGTH_SHORT).show()
        }
    }

    private fun takePhoto() {
        if (isCameraAvailable()) {
            val cameraDialog = CameraDialogFragment.newInstance { photoPath ->
                photoPath?.let { path ->
                    this.fotoPath = path
                    ivFoto.setImageBitmap(BitmapFactory.decodeFile(path))
                    Log.d("EvaluacionGeneralFragment", "Photo loaded from dialog: $path")
                } ?: run {
                    Toast.makeText(requireContext(), "No se pudo capturar la foto", Toast.LENGTH_SHORT).show()
                }
            }
            cameraDialog.show(childFragmentManager, CameraDialogFragment.TAG)
        } else {
            Toast.makeText(requireContext(), "No hay cámara disponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSignatureBottomSheet() {
        val signatureBottomSheet = SignatureBottomSheetFragment.newInstance { signaturePath ->
            signaturePath?.let { path ->
                firmaPath = path
                ivSignature.setImageBitmap(BitmapFactory.decodeFile(path))
                ivSignature.visibility = View.VISIBLE
                tvTapToSign.visibility = View.GONE
                btnClearFirma.visibility = View.VISIBLE
            } ?: run {
                Toast.makeText(requireContext(), "Error al guardar la firma", Toast.LENGTH_SHORT).show()
            }
        }
        signatureBottomSheet.show(childFragmentManager, SignatureBottomSheetFragment.TAG)
    }
}