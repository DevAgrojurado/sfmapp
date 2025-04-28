package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluacionfragmentsform

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.agrojurado.sfmappv2.databinding.FragmentInformacionGeneralBinding
import com.agrojurado.sfmappv2.domain.model.Lote
import com.agrojurado.sfmappv2.domain.model.Operario
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.shared.SharedSelectionViewModel
import com.agrojurado.sfmappv2.utils.LocationPermissionHandler
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class InformacionGeneralFragment : Fragment() {

    private var _binding: FragmentInformacionGeneralBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EvaluacionViewModel by activityViewModels()
    private val sharedViewModel: SharedSelectionViewModel by activityViewModels()
    private var operarios: List<Pair<String, Operario>> = emptyList()
    private var lotes: List<Pair<String, Lote>> = emptyList()
    private val handler = Handler(Looper.getMainLooper())
    private var checkPalmRunnable: Runnable? = null
    private lateinit var locationHandler: LocationPermissionHandler

    private var isUpdatingFromSharedViewModel = false
    private var operariosSpinnerReady = false
    private var lotesSpinnerReady = false
    // Variables para IDs iniciales pasados por argumentos
    private var initialOperarioId: Int? = null
    private var initialLoteId: Int? = null
    private var initialSeccion: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("InformacionGeneralFragment", "onCreateView")
        _binding = FragmentInformacionGeneralBinding.inflate(inflater, container, false)
        
        // Leer argumentos aquí
        arguments?.let {
            initialOperarioId = it.getInt("operarioId", -1).let { id -> if (id == -1) null else id }
            initialLoteId = it.getInt("loteId", -1).let { id -> if (id == -1) null else id }
            initialSeccion = it.getInt("seccion", -1).let { id -> if (id == -1) null else id }
            Log.d("InformacionGeneralFragment", "Argumentos recibidos: Operario=$initialOperarioId, Lote=$initialLoteId, Seccion=$initialSeccion")
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInitialValues()
        locationHandler = LocationPermissionHandler(
            context = requireContext(),
            activity = requireActivity(),
            onLocationReceived = { location ->
                if (isAdded) { // Verificar si el fragmento está adjunto
                    viewModel.setUbicacion(location)
                }
            },
            onPermissionDenied = {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
                }
            },
            onGPSDisabled = {
                if (isAdded) {
                    Toast.makeText(requireContext(), "GPS es necesario para obtener la ubicación", Toast.LENGTH_SHORT).show()
                }
            }
        )
        sharedViewModel.ensureDataLoaded()
        setupObservers()
        savedInstanceState?.let {
            if (_binding != null) {
                binding.etPalma.setText(it.getString("palma", ""))
                binding.etUbicacion.setText(it.getString("ubicacion", ""))
                val inflorescencia = it.getInt("inflorescencia", 0)
                if (inflorescencia > 0) setInflorescencia(inflorescencia)
            }
        }
        if (isAdded) {
            locationHandler.requestLocation()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("palma", binding.etPalma.text.toString())
        outState.putString("ubicacion", binding.etUbicacion.text.toString())
        val inflorescencia = binding.run {
            val toggleButtons = listOf(toggle1, toggle2, toggle3, toggle4, toggle5)
            toggleButtons.indexOfFirst { it.isChecked }.let { if (it == -1) 0 else it + 1 }
        }
        outState.putInt("inflorescencia", inflorescencia)
    }

    private fun setupInitialValues() {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val currentWeek = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)

        with(binding) {
            etFecha.setText(currentDate)
            etHora.setText(currentTime)
            etSemana.setText(currentWeek.toString())
            tvTotalPalmas.visibility = View.VISIBLE
        }
    }

    private fun setupObservers() {
        viewModel.loggedInUser.observe(viewLifecycleOwner) { user ->
            if (_binding != null) {
                binding.tvEvaluador.text = user?.let { "${it.codigo} - ${it.nombre}" } ?: "Usuario no disponible"
            }
        }

        sharedViewModel.operarios.observe(viewLifecycleOwner) { operariosList ->
            if (operariosList.isNotEmpty()) {
                operarios = operariosList
                setupOperariosSpinner(operariosList)
                // Setup listeners solo después de configurar el spinner y tener los datos
                setupListeners()
                operariosSpinnerReady = true
                Log.d("InformacionGeneralFragment", "Operarios Spinner listo")
                // Intentar aplicar selecciones INICIALES ahora que el spinner está listo
                applyInitialArguments()
            }
        }

        viewModel.lastUsedOperarioId.observe(viewLifecycleOwner) { lastUsedOperarioId ->
            lastUsedOperarioId?.let { id ->
                val position = operarios.indexOfFirst { it.second.id == id }
                if (position != -1 && _binding != null) {
                    binding.spinnerPolinizador.setSelection(position)
                }
            }
        }

        viewModel.totalPalmas.observe(viewLifecycleOwner) { total ->
            if (_binding != null) {
                binding.tvTotalPalmas.text = "$total"
            }
        }

        viewModel.ubicacion.observe(viewLifecycleOwner) { ubicacion ->
            if (_binding != null) {
                binding.etUbicacion.setText(ubicacion)
            }
        }

        sharedViewModel.lotes.observe(viewLifecycleOwner) { lotesList ->
            if (lotesList.isNotEmpty()) {
                lotes = lotesList
                setupLotesSpinner(lotesList)
                lotesSpinnerReady = true
                Log.d("InformacionGeneralFragment", "Lotes Spinner listo")
                // Intentar aplicar selecciones INICIALES ahora que el spinner está listo
                applyInitialArguments()
            }
        }

        viewModel.lastUsedLoteId.observe(viewLifecycleOwner) { lastUsedLoteId ->
            lastUsedLoteId?.let { id ->
                val position = lotes.indexOfFirst { it.second.id == id }
                if (position != -1 && _binding != null) {
                    binding.spinnerLote.setSelection(position)
                }
            }
        }

        viewModel.palmExists.observe(viewLifecycleOwner) { exists ->
            if (_binding != null) {
                binding.etPalma.error = if (exists) "Esta palma ya existe en esta sección" else null
            }
        }

        viewModel.validationErrors.observe(viewLifecycleOwner) { errorFields ->
            if (_binding != null) {
                clearAllErrors()
                errorFields.forEach { fieldName ->
                    when (fieldName) {
                        "etFecha" -> binding.etFecha.error = "Campo requerido"
                        "etHora" -> binding.etHora.error = "Campo requerido"
                        "etSemana" -> binding.etSemana.error = "Campo requerido"
                        "spinnerPolinizador" -> (binding.spinnerPolinizador.selectedView as? TextView)?.error = "Campo requerido"
                        "spinnerLote" -> (binding.spinnerLote.selectedView as? TextView)?.error = "Campo requerido"
                        "etSeccion" -> binding.etSeccion.error = "Campo requerido"
                        "ubicacion" -> binding.etUbicacion.error = "Campo requerido"
                    }
                }
            }
        }
    }

    private fun clearAllErrors() {
        with(binding) {
            etFecha.error = null
            etHora.error = null
            etSemana.error = null
            (spinnerPolinizador.selectedView as? TextView)?.error = null
            (spinnerLote.selectedView as? TextView)?.error = null
            etSeccion.error = null
            etPalma.error = null
            etUbicacion.error = null
        }
    }

    private fun setupOperariosSpinner(operariosList: List<Pair<String, Operario>>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            operariosList.map { it.first }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerPolinizador.adapter = adapter
    }

    private fun setupLotesSpinner(lotesList: List<Pair<String, Lote>>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            lotesList.map { it.first }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerLote.adapter = adapter
    }

    private fun setupListeners() {
        with(binding) {
            val toggleButtons = listOf(toggle1, toggle2, toggle3, toggle4, toggle5)
            toggleButtons.forEachIndexed { index, toggleButton ->
                toggleButton.setOnCheckedChangeListener { buttonView, isChecked ->
                    try {
                        if (isChecked) {
                            toggleButtons.forEach { btn -> if (btn != buttonView) btn.isChecked = false }
                            viewModel.setInflorescencia(index + 1)
                        } else if (!toggleButtons.any { it.isChecked }) {
                            viewModel.setInflorescencia(0)
                        }
                    } catch (e: Exception) {
                        Log.e("InformacionGeneralFragment", "Error in toggleButton listener: ${e.message}", e)
                    }
                }
            }

            spinnerPolinizador.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    try {
                        if (!isUpdatingFromSharedViewModel && operarios.isNotEmpty()) {
                            val selectedOperario = operarios[position].second
                            viewModel.updateTotalPalmas(selectedOperario.id, viewModel.getEvaluacionGeneralId())
                            checkPalmExists()
                        }
                    } catch (e: Exception) {
                        Log.e("InformacionGeneralFragment", "Error in spinnerPolinizador listener: ${e.message}", e)
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            spinnerLote.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    try {
                        if (!isUpdatingFromSharedViewModel && lotes.isNotEmpty()) {
                            val selectedLote = lotes[position].second
                            checkPalmExists()
                        }
                    } catch (e: Exception) {
                        Log.e("InformacionGeneralFragment", "Error in spinnerLote listener: ${e.message}", e)
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            etSeccion.doAfterTextChanged { text ->
                try {
                    if (!isUpdatingFromSharedViewModel) {
                        val seccion = text.toString().toIntOrNull()
                        seccion?.let { checkPalmExists() }
                    }
                } catch (e: Exception) {
                    Log.e("InformacionGeneralFragment", "Error in etSeccion listener: ${e.message}", e)
                }
            }

            etPalma.doAfterTextChanged { text ->
                checkPalmRunnable?.let { handler.removeCallbacks(it) }
                checkPalmRunnable = Runnable { checkPalmExists() }
                handler.postDelayed(checkPalmRunnable!!, 100)
            }
        }
    }

    private fun checkPalmExists() {
        if (!isAdded) {
            Log.w("InformacionGeneralFragment", "Fragment not attached, skipping checkPalmExists")
            return
        }
        with(binding) {
            val semana = etSemana.text.toString().toIntOrNull()
            val idLote = lotes.getOrNull(spinnerLote.selectedItemPosition)?.second?.id
            val palma = etPalma.text.toString().toIntOrNull()
            val idPolinizador = operarios.getOrNull(spinnerPolinizador.selectedItemPosition)?.second?.id
            val seccion = etSeccion.text.toString().toIntOrNull()
            val evaluacionGeneralId = viewModel.getEvaluacionGeneralId()

            if (semana != null && idLote != null && palma != null && idPolinizador != null && seccion != null && evaluacionGeneralId != null) {
                viewModel.checkPalmExists(semana, idLote, palma, idPolinizador, seccion, evaluacionGeneralId)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        locationHandler.handleRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        locationHandler.handleActivityResult(requestCode)
    }

    fun setInflorescencia(value: Int) {
        if (_binding != null) {
            with(binding) {
                val toggleButtons = listOf(toggle1, toggle2, toggle3, toggle4, toggle5)
                toggleButtons.forEachIndexed { index, toggleButton ->
                    toggleButton.isChecked = (index + 1) == value
                }
            }
        }
    }

    fun getValues(): Map<String, Any?> {
        return _binding?.let { binding ->
            val toggleButtons = listOf(binding.toggle1, binding.toggle2, binding.toggle3, binding.toggle4, binding.toggle5)
            val inflorescencia = toggleButtons.indexOfFirst { it.isChecked }.let { if (it == -1) 0 else it + 1 }

            mapOf(
                "etFecha" to binding.etFecha.text.toString(),
                "etHora" to binding.etHora.text.toString(),
                "etSemana" to binding.etSemana.text.toString().toIntOrNull(),
                "tvEvaluador" to binding.tvEvaluador.text.toString(),
                "spinnerPolinizador" to operarios.getOrNull(binding.spinnerPolinizador.selectedItemPosition)?.second?.id,
                "spinnerLote" to lotes.getOrNull(binding.spinnerLote.selectedItemPosition)?.second?.id,
                "etSeccion" to binding.etSeccion.text.toString().toIntOrNull(),
                "etPalma" to binding.etPalma.text.toString().toIntOrNull(),
                "etUbicacion" to binding.etUbicacion.text.toString(),
                "inflorescencia" to inflorescencia
            )
        } ?: mapOf()
    }

    private fun applyInitialArguments() {
        // Solo aplicar si ambos spinners están listos y los argumentos fueron leídos
        if (!operariosSpinnerReady || !lotesSpinnerReady) {
            Log.d("InformacionGeneralFragment", "applyInitialArguments: Esperando que ambos spinners estén listos (Operarios: $operariosSpinnerReady, Lotes: $lotesSpinnerReady)")
            return
        }
        
        if (_binding == null) {
            Log.w("InformacionGeneralFragment", "applyInitialArguments: Binding es nulo, no se pueden aplicar selecciones")
            return
        }
        
        Log.d("InformacionGeneralFragment", "applyInitialArguments: Aplicando selecciones desde Argumentos...")
        
        initialOperarioId?.let { id ->
            Log.d("InformacionGeneralFragment", "Intentando aplicar Operario ID inicial: $id")
            val position = operarios.indexOfFirst { it.second.id == id }
            Log.d("InformacionGeneralFragment", "Posición encontrada para Operario ID inicial $id: $position")
            if (position != -1 && binding.spinnerPolinizador.adapter != null && position < binding.spinnerPolinizador.adapter.count) {
                if (binding.spinnerPolinizador.selectedItemPosition != position) {
                    Log.d("InformacionGeneralFragment", "Estableciendo selección de Polinizador inicial en posición: $position")
                    isUpdatingFromSharedViewModel = true
                    binding.spinnerPolinizador.setSelection(position, false) // false para no animar
                    isUpdatingFromSharedViewModel = false
                } else {
                    Log.d("InformacionGeneralFragment", "Polinizador ya está en la posición inicial correcta ($position)")
                }
            } else {
                 Log.w("InformacionGeneralFragment", "No se pudo establecer la selección inicial para Operario ID $id. Posición: $position, Adapter Count: ${binding.spinnerPolinizador.adapter?.count}")
            }
        } ?: Log.d("InformacionGeneralFragment", "No hay Operario ID inicial para aplicar.")

        initialLoteId?.let { id ->
             Log.d("InformacionGeneralFragment", "Intentando aplicar Lote ID inicial: $id")
            val position = lotes.indexOfFirst { it.second.id == id }
            Log.d("InformacionGeneralFragment", "Posición encontrada para Lote ID inicial $id: $position")
            if (position != -1 && binding.spinnerLote.adapter != null && position < binding.spinnerLote.adapter.count) {
                if (binding.spinnerLote.selectedItemPosition != position) {
                     Log.d("InformacionGeneralFragment", "Estableciendo selección de Lote inicial en posición: $position")
                    isUpdatingFromSharedViewModel = true
                    binding.spinnerLote.setSelection(position, false) // false para no animar
                    isUpdatingFromSharedViewModel = false
                } else {
                     Log.d("InformacionGeneralFragment", "Lote ya está en la posición inicial correcta ($position)")
                }
            } else {
                Log.w("InformacionGeneralFragment", "No se pudo establecer la selección inicial para Lote ID $id. Posición: $position, Adapter Count: ${binding.spinnerLote.adapter?.count}")
            }
        } ?: Log.d("InformacionGeneralFragment", "No hay Lote ID inicial para aplicar.")

        initialSeccion?.let {
            Log.d("InformacionGeneralFragment", "Intentando aplicar Sección inicial: $it")
            if (binding.etSeccion.text.toString() != it.toString()) {
                isUpdatingFromSharedViewModel = true
                binding.etSeccion.setText(it.toString())
                isUpdatingFromSharedViewModel = false
                 Log.d("InformacionGeneralFragment", "Sección inicial establecida a: $it")
            } else {
                 Log.d("InformacionGeneralFragment", "Sección inicial ya está establecida a: $it")
            }
        } ?: Log.d("InformacionGeneralFragment", "No hay Sección inicial para aplicar.")
    }

    override fun onResume() {
        super.onResume()
        // Ya no es necesario aplicar selecciones aquí
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("InformacionGeneralFragment", "onDestroyView")
        checkPalmRunnable?.let { handler.removeCallbacks(it) }
        _binding = null
    }
}