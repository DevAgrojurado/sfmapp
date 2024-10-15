package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.Operario
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class InformacionGeneralFragment : Fragment() {

    private lateinit var rgInflorescencia: RadioGroup
    private lateinit var etFecha: TextInputEditText
    private lateinit var etHora: TextInputEditText
    private lateinit var etSemana: TextInputEditText
    private lateinit var tvEvaluador: TextView
    private lateinit var spinnerPolinizador: Spinner
    private lateinit var etLote: TextInputEditText
    private lateinit var etSeccion: TextInputEditText
    private lateinit var etPalma: TextInputEditText
    private lateinit var tvTotalPalmas: TextView
    private lateinit var etUbicacion: TextInputEditText
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val viewModel: EvaluacionViewModel by activityViewModels()
    private var operarios: List<Pair<String, Operario>> = emptyList()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_informacion_general, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupInitialValues()
        setupObservers()
        setupListeners()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        requestLocationPermission()

        viewModel.loadOperarios()
    }

    private fun initializeViews(view: View) {
        rgInflorescencia = view.findViewById(R.id.rgInflorescencia)
        etFecha = view.findViewById(R.id.etFecha)
        etHora = view.findViewById(R.id.etHora)
        etSemana = view.findViewById(R.id.etSemana)
        tvEvaluador = view.findViewById(R.id.tvEvaluador)
        spinnerPolinizador = view.findViewById(R.id.spinnerPolinizador)
        etLote = view.findViewById(R.id.etLote)
        etSeccion = view.findViewById(R.id.etSeccion)
        etPalma = view.findViewById(R.id.etPalma)
        tvTotalPalmas = view.findViewById(R.id.tvTotalPalmas)
        etUbicacion = view.findViewById(R.id.etUbicacion)
        tvTotalPalmas.visibility = View.VISIBLE
    }

    private fun setupInitialValues() {
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val currentWeek = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)

        etFecha.setText(currentDate)
        etHora.setText(currentTime)
        etSemana.setText(currentWeek.toString())
    }

    private fun setupObservers() {
        viewModel.loggedInUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                tvEvaluador.text = "${it.codigo} - ${it.nombre}"
            } ?: run {
                tvEvaluador.text = "Usuario no disponible"
            }
        }

        viewModel.operarios.observe(viewLifecycleOwner) { operariosList ->
            operarios = operariosList
            setupOperariosSpinner(operariosList)
        }

        viewModel.lastUsedOperarioId.observe(viewLifecycleOwner) { lastUsedOperarioId ->
            lastUsedOperarioId?.let { id ->
                val position = operarios.indexOfFirst { it.second.id == id }
                if (position != -1) {
                    spinnerPolinizador.setSelection(position)
                }
            }
        }

        viewModel.totalPalmas.observe(viewLifecycleOwner) { total ->
            tvTotalPalmas.text = "$total"
        }

        viewModel.ubicacion.observe(viewLifecycleOwner) { ubicacion ->
            etUbicacion.setText(ubicacion)
        }
    }

    private fun setupOperariosSpinner(operariosList: List<Pair<String, Operario>>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, operariosList.map { it.first })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPolinizador.adapter = adapter

        spinnerPolinizador.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedOperario = operariosList[position].second
                viewModel.updateTotalPalmas(selectedOperario.id)
                checkPalmExists()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No hacer nada
            }
        }
    }

    private fun setupListeners() {
        rgInflorescencia.setOnCheckedChangeListener { _, checkedId ->
            val selectedInflorescencia = when (checkedId) {
                R.id.rb1 -> 1
                R.id.rb2 -> 2
                R.id.rb3 -> 3
                R.id.rb4 -> 4
                R.id.rb5 -> 5
                else -> 0
            }
            viewModel.setInflorescencia(selectedInflorescencia)
        }

        etSemana.doAfterTextChanged { checkPalmExists() }
        etLote.doAfterTextChanged { checkPalmExists() }
        etSeccion.doAfterTextChanged { checkPalmExists() }
        etPalma.doAfterTextChanged { checkPalmExists() }
    }

    private fun checkPalmExists() {
        val semana = etSemana.text.toString().toIntOrNull()
        val lote = etLote.text.toString().toIntOrNull()
        val palma = etPalma.text.toString().toIntOrNull()
        val idPolinizador = (spinnerPolinizador.selectedItem as? Pair<String, Operario>)?.second?.id

        if (semana != null && lote != null && palma != null && idPolinizador != null) {
            viewModel.checkPalmExists(semana, lote, palma, idPolinizador)
        }
    }

    private fun requestLocationPermission() {
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
            getLastLocation()
        }
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val ubicacion = "${it.latitude},${it.longitude}"
                        viewModel.setUbicacion(ubicacion)
                    }
                }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getLastLocation()
                } else {
                    Toast.makeText(requireContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    fun getValues(): Map<String, Any> {
        return mapOf(
            "etFecha" to etFecha.text.toString().ifEmpty { throw IllegalArgumentException("La fecha no puede estar vacía") },
            "etHora" to etHora.text.toString().ifEmpty { throw IllegalArgumentException("La hora no puede estar vacía") },
            "etSemana" to (etSemana.text.toString().toIntOrNull() ?: throw IllegalArgumentException("La semana debe ser un número")),
            "tvEvaluador" to tvEvaluador.text.toString().ifEmpty { throw IllegalArgumentException("El evaluador no puede estar vacío") },
            "spinnerPolinizador" to (operarios.getOrNull(spinnerPolinizador.selectedItemPosition)?.second?.id ?: throw IllegalArgumentException("Debe seleccionar un polinizador válido")),
            "etLote" to (etLote.text.toString().toIntOrNull() ?: throw IllegalArgumentException("El lote debe ser un número")),
            "etSeccion" to (etSeccion.text.toString().toIntOrNull() ?: throw IllegalArgumentException("La sección debe ser un número")),
            "etPalma" to (etPalma.text.toString().toIntOrNull() ?: throw IllegalArgumentException("La palma debe ser un número")),
            "etUbicacion" to etUbicacion.text.toString().ifEmpty { throw IllegalArgumentException("La ubicación no puede estar vacía") }
        )
    }
}