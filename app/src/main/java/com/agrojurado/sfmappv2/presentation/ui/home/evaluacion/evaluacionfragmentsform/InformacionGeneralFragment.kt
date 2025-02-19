package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluacionfragmentsform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.databinding.FragmentInformacionGeneralBinding
import com.agrojurado.sfmappv2.domain.model.Lote
import com.agrojurado.sfmappv2.domain.model.Operario
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class InformacionGeneralFragment : Fragment(), LocationListener {

    private var _binding: FragmentInformacionGeneralBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EvaluacionViewModel by activityViewModels()
    private var operarios: List<Pair<String, Operario>> = emptyList()
    private var lotes: List<Pair<String, Lote>> = emptyList()
    private lateinit var locationManager: LocationManager
    private val handler = Handler(Looper.getMainLooper())
    private var checkPalmRunnable: Runnable? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val GPS_REQUEST_CODE = 2
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInformacionGeneralBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInitialValues()
        setupObservers()
        setupListeners()

        locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        checkLocationPermission()

        viewModel.loadOperarios()
        viewModel.loadLotes()

        // Observar errores de validación
        viewModel.validationErrors.observe(viewLifecycleOwner) { errorFields ->
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

    private fun setupInitialValues() {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
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
            binding.tvEvaluador.text = user?.let { "${it.codigo} - ${it.nombre}" } ?: "Usuario no disponible"
        }

        viewModel.operarios.observe(viewLifecycleOwner) { operariosList ->
            operarios = operariosList
            setupOperariosSpinner(operariosList)
        }

        viewModel.lastUsedOperarioId.observe(viewLifecycleOwner) { lastUsedOperarioId ->
            lastUsedOperarioId?.let { id ->
                val position = operarios.indexOfFirst { it.second.id == id }
                if (position != -1) {
                    binding.spinnerPolinizador.setSelection(position)
                }
            }
        }

        viewModel.totalPalmas.observe(viewLifecycleOwner) { total ->
            binding.tvTotalPalmas.text = "$total"
        }

        viewModel.ubicacion.observe(viewLifecycleOwner) { ubicacion ->
            binding.etUbicacion.setText(ubicacion)
        }

        viewModel.lotes.observe(viewLifecycleOwner) { lotesList ->
            lotes = lotesList
            setupLotesSpinner(lotesList)
        }

        viewModel.lastUsedLoteId.observe(viewLifecycleOwner) { lastUsedLoteId ->
            lastUsedLoteId?.let { id ->
                val position = lotes.indexOfFirst { it.second.id == id }
                if (position != -1) {
                    binding.spinnerLote.setSelection(position)
                }
            }
        }

        viewModel.palmExists.observe(viewLifecycleOwner) { exists ->
            binding.etPalma.error = if (exists) "Esta palma ya existe en esta sección" else null
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

        binding.spinnerPolinizador.apply {
            this.adapter = adapter
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedOperario = operariosList[position].second
                    viewModel.updateTotalPalmas(selectedOperario.id)
                    checkPalmExists()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun setupLotesSpinner(lotesList: List<Pair<String, Lote>>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            lotesList.map { it.first }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerLote.apply {
            this.adapter = adapter
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    checkPalmExists()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun setupListeners() {
        with(binding) {
            val toggleButtons = listOf(toggle1, toggle2, toggle3, toggle4, toggle5)

            toggleButtons.forEachIndexed { index, toggleButton ->
                toggleButton.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        toggleButtons.forEach { btn ->
                            if (btn != buttonView) btn.isChecked = false
                        }
                        viewModel.setInflorescencia(index + 1)
                    } else {
                        if (!toggleButtons.any { it.isChecked }) {
                            viewModel.setInflorescencia(0)
                        }
                    }
                }
            }

            etSemana.doAfterTextChanged { checkPalmExists() }
            etSeccion.doAfterTextChanged { checkPalmExists() }
            etPalma.doAfterTextChanged { text ->
                checkPalmRunnable?.let { handler.removeCallbacks(it) }
                checkPalmRunnable = Runnable { checkPalmExists() }
                handler.postDelayed(checkPalmRunnable!!, 100)
            }
        }
    }

    private fun checkPalmExists() {
        with(binding) {
            val semana = etSemana.text.toString().toIntOrNull()
            val idLote = lotes.getOrNull(spinnerLote.selectedItemPosition)?.second?.id
            val palma = etPalma.text.toString().toIntOrNull()
            val idPolinizador = operarios.getOrNull(spinnerPolinizador.selectedItemPosition)?.second?.id
            val seccion = etSeccion.text.toString().toIntOrNull()

            if (semana != null && idLote != null && palma != null && idPolinizador != null && seccion != null) {
                viewModel.checkPalmExists(semana, idLote, palma, idPolinizador, seccion)
            }
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            checkGPSEnabled()
        }
    }

    private fun checkGPSEnabled() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGPSDisabledAlert()
        } else {
            startLocationUpdates()
        }
    }

    private fun showGPSDisabledAlert() {
        AlertDialog.Builder(requireContext())
            .setMessage("GPS está desactivado. ¿Desea activarlo?")
            .setCancelable(false)
            .setPositiveButton("Sí") { _, _ ->
                startActivityForResult(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                    GPS_REQUEST_CODE
                )
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
                Toast.makeText(
                    requireContext(),
                    "GPS es necesario para obtener la ubicación",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L,
                10f,
                this
            )
        }
    }

    override fun onLocationChanged(location: Location) {
        val ubicacion = "${location.latitude},${location.longitude}"
        viewModel.setUbicacion(ubicacion)
        locationManager.removeUpdates(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkGPSEnabled()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Permiso de ubicación denegado",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GPS_REQUEST_CODE) {
            checkGPSEnabled()
        }
    }

    fun setInflorescencia(value: Int) {
        with(binding) {
            val toggleButtons = listOf(toggle1, toggle2, toggle3, toggle4, toggle5)
            toggleButtons.forEachIndexed { index, toggleButton ->
                toggleButton.isChecked = (index + 1) == value
            }
        }
    }

    fun getValues(): Map<String, Any?> {
        return _binding?.let { binding ->
            val toggleButtons = listOf(
                binding.toggle1,
                binding.toggle2,
                binding.toggle3,
                binding.toggle4,
                binding.toggle5
            )
            val inflorescencia = toggleButtons.indexOfFirst { it.isChecked }.let {
                if (it == -1) 0 else it + 1
            }

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
        } ?: mapOf() // Retornar un mapa vacío si el binding es null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}