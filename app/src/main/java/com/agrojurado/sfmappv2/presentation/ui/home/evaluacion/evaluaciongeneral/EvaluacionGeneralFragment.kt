package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluaciongeneral

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

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
    private lateinit var spinnerPolinizador: Spinner
    private lateinit var spinnerLote: Spinner
    private lateinit var etSeccion: EditText
    private lateinit var ivFoto: ImageView
    private lateinit var btnAddFoto: Button
    private lateinit var signatureContainer: LinearLayout
    private lateinit var ivSignature: ImageView
    private lateinit var tvTapToSign: TextView
    private lateinit var btnClearFirma: Button
    private lateinit var btnSaveFirma: Button

    private var isUpdatingFromSharedViewModel = false
    private var fotoPath: String? = null
    private var firmaPath: String? = null

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

        // Enable options menu in this fragment
        setHasOptionsMenu(true)

        if (viewModel.temporaryEvaluacionId.value == null) {
            viewModel.initTemporaryEvaluacion()
        }
        viewModel.loadEvaluacionesIndividuales()
        sharedViewModel.loadOperarios()
        sharedViewModel.loadLotes()

        setupObservers()
        setupListeners()

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

    // Override to handle toolbar back button presses
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Handle the Up/Home button in the toolbar
                handleCancelAction()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("fotoPath", fotoPath)
        outState.putString("firmaPath", firmaPath)
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewEvaluaciones)
        btnGuardarGeneral = view.findViewById(R.id.btnGuardarGeneral)
        btnCancelar = view.findViewById(R.id.btnCancelar)
        fabAddEvaluacion = view.findViewById(R.id.fabAddEvaluacion)
        tvNoEvaluaciones = view.findViewById(R.id.tvNoEvaluaciones)
        tvTotalEvaluaciones = view.findViewById(R.id.tvTotalEvaluaciones)
        spinnerPolinizador = view.findViewById(R.id.spinnerPolinizador)
        spinnerLote = view.findViewById(R.id.spinnerLote)
        etSeccion = view.findViewById(R.id.etSeccion)
        ivFoto = view.findViewById(R.id.ivFoto)
        btnAddFoto = view.findViewById(R.id.btnAddFoto)
        signatureContainer = view.findViewById(R.id.signatureContainer)
        ivSignature = view.findViewById(R.id.ivSignature)
        tvTapToSign = view.findViewById(R.id.tvTapToSign)
        btnClearFirma = view.findViewById(R.id.btnClearFirma)
        btnSaveFirma = view.findViewById(R.id.btnSaveFirma)
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
        sharedViewModel.operarios.observe(viewLifecycleOwner) { operariosList ->
            if (operariosList.isNotEmpty()) {
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    operariosList.map { it.first }
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerPolinizador.adapter = adapter
                sharedViewModel.selectedOperarioId.value?.let { id ->
                    val position = operariosList.indexOfFirst { it.second.id == id }
                    if (position != -1) {
                        isUpdatingFromSharedViewModel = true
                        spinnerPolinizador.setSelection(position)
                        isUpdatingFromSharedViewModel = false
                    }
                }
            }
        }

        sharedViewModel.selectedOperarioId.observe(viewLifecycleOwner) { id ->
            id?.let {
                val operariosList = sharedViewModel.operarios.value ?: return@let
                val position = operariosList.indexOfFirst { pair -> pair.second.id == it }
                if (position != -1 && spinnerPolinizador.selectedItemPosition != position) {
                    isUpdatingFromSharedViewModel = true
                    spinnerPolinizador.setSelection(position)
                    isUpdatingFromSharedViewModel = false
                }
            }
        }

        sharedViewModel.lotes.observe(viewLifecycleOwner) { lotesList ->
            if (lotesList.isNotEmpty()) {
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    lotesList.map { it.first }
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerLote.adapter = adapter
                sharedViewModel.selectedLoteId.value?.let { id ->
                    val position = lotesList.indexOfFirst { it.second.id == id }
                    if (position != -1) {
                        isUpdatingFromSharedViewModel = true
                        spinnerLote.setSelection(position)
                        isUpdatingFromSharedViewModel = false
                    }
                }
            }
        }

        sharedViewModel.selectedLoteId.observe(viewLifecycleOwner) { id ->
            id?.let {
                val lotesList = sharedViewModel.lotes.value ?: return@let
                val position = lotesList.indexOfFirst { pair -> pair.second.id == it }
                if (position != -1 && spinnerLote.selectedItemPosition != position) {
                    isUpdatingFromSharedViewModel = true
                    spinnerLote.setSelection(position)
                    isUpdatingFromSharedViewModel = false
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
            // Ya no llamamos a guardarSeleccionesActuales aquí para este flujo
            // guardarSeleccionesActuales()
            
            // Obtener los IDs seleccionados directamente
            val selectedOperarioId = sharedViewModel.operarios.value?.getOrNull(spinnerPolinizador.selectedItemPosition)?.second?.id
            val selectedLoteId = sharedViewModel.lotes.value?.getOrNull(spinnerLote.selectedItemPosition)?.second?.id
            val selectedSeccion = etSeccion.text.toString().toIntOrNull()
            
            Log.d("EvaluacionGeneralFragment", "Iniciando EvaluacionActivity con Operario=$selectedOperarioId, Lote=$selectedLoteId, Seccion=$selectedSeccion")

            if (checkCameraPermission()) {
                val intent = Intent(requireContext(), EvaluacionActivity::class.java).apply {
                    putExtra("evaluacionGeneralId", viewModel.temporaryEvaluacionId.value ?: -1)
                    // Pasar los IDs como extras
                    selectedOperarioId?.let { putExtra("operarioId", it) }
                    selectedLoteId?.let { putExtra("loteId", it) }
                    selectedSeccion?.let { putExtra("seccion", it) }
                }
                startActivity(intent)
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

        spinnerPolinizador.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isUpdatingFromSharedViewModel) {
                    val operarioId = sharedViewModel.operarios.value?.get(position)?.second?.id
                    operarioId?.let { sharedViewModel.setSelectedOperarioId(it); viewModel.setSelectedPolinizadorId(it) }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                sharedViewModel.setSelectedOperarioId(null)
                viewModel.setSelectedPolinizadorId(0)
            }
        }

        spinnerLote.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isUpdatingFromSharedViewModel) {
                    val loteId = sharedViewModel.lotes.value?.get(position)?.second?.id
                    loteId?.let { sharedViewModel.setSelectedLoteId(it); viewModel.setSelectedLoteId(it) }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                sharedViewModel.setSelectedLoteId(null)
                viewModel.setSelectedLoteId(0)
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

        btnSaveFirma.setOnClickListener {
            // No action needed, signature is saved by the bottomsheet
        }
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
        if (tempId != null) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancelar Evaluación")
                .setMessage("¿Desea cancelar la evaluación en curso? Se perderán todos los datos no guardados.")
                .setPositiveButton("Sí") { _, _ ->
                    if (isAdded) { // Verifica si el fragmento está adjunto
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
                findNavController().popBackStack(R.id.listaEvaluacionFragment, false)
            } else {
                Log.w("EvaluacionGeneralFragment", "Fragment not attached, skipping navigation")
            }
        }
    }

    private fun guardarSeleccionesActuales() {
        try {
            // Actualizar el ViewModel de la evaluación general
            val operarioPosition = spinnerPolinizador.selectedItemPosition
            if (operarioPosition != Spinner.INVALID_POSITION) {
                sharedViewModel.operarios.value?.get(operarioPosition)?.second?.id?.let {
                    viewModel.setSelectedPolinizadorId(it)
                    // Actualizar también el SharedViewModel explícitamente
                    sharedViewModel.setSelectedOperarioId(it)
                }
            }
            val lotePosition = spinnerLote.selectedItemPosition
            if (lotePosition != Spinner.INVALID_POSITION) {
                sharedViewModel.lotes.value?.get(lotePosition)?.second?.id?.let {
                    viewModel.setSelectedLoteId(it)
                    // Actualizar también el SharedViewModel explícitamente
                    sharedViewModel.setSelectedLoteId(it)
                }
            }
            val seccion = etSeccion.text.toString().toIntOrNull()
            sharedViewModel.setSelectedSeccion(seccion)
            
            Log.d("EvaluacionGeneralFragment", "Selecciones guardadas en SharedViewModel: Operario=${sharedViewModel.selectedOperarioId.value}, Lote=${sharedViewModel.selectedLoteId.value}, Seccion=${sharedViewModel.selectedSeccion.value}")
            
        } catch (e: Exception) {
            Log.e("EvaluacionGeneralFragment", "Error al guardar selecciones", e)
            Toast.makeText(requireContext(), "Error al guardar selecciones: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateEvaluacionesList(evaluaciones: List<EvaluacionPolinizacion>) {
        evaluacionesAdapter.submitList(evaluaciones)
        tvTotalEvaluaciones.text = "Total: ${evaluaciones.size} eventos"
        tvNoEvaluaciones.visibility = if (evaluaciones.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (evaluaciones.isEmpty()) View.GONE else View.VISIBLE
        btnGuardarGeneral.isEnabled = evaluaciones.isNotEmpty()
        btnCancelar.isEnabled = true
    }

    private fun showEvaluacionDetails(evaluacion: EvaluacionPolinizacion) {
        Toast.makeText(requireContext(), "Lote: ${evaluacion.idlote}, Palma: ${evaluacion.palma ?: "N/A"}, Sección: ${evaluacion.seccion}", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadEvaluacionesIndividuales()
    }

    // Método helper para encontrar vistas
    private fun <T : View> findViewById(id: Int): T? {
        return view?.findViewById(id)
    }

    // Implementación de métodos faltantes
    private fun guardarEvaluacion() {
        if (viewModel.hasEvaluacionesIndividuales()) {
            if (firmaPath == null) {
                Toast.makeText(requireContext(), "Por favor, agregue una firma antes de guardar", Toast.LENGTH_SHORT).show()
                return
            }
            viewModel.guardarEvaluacionGeneral(fotoPath, firmaPath, requireContext())
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