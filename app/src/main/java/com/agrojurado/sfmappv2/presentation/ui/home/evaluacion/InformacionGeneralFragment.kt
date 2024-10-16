package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.Operario
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class InformacionGeneralFragment : Fragment(), LocationListener {

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
    private lateinit var locationManager: LocationManager

    private val viewModel: EvaluacionViewModel by activityViewModels()
    private var operarios: List<Pair<String, Operario>> = emptyList()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val GPS_REQUEST_CODE = 2
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

        locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        checkLocationPermission()

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
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setMessage("GPS está desactivado. ¿Desea activarlo?")
            .setCancelable(false)
            .setPositiveButton("Sí") { _, _ ->
                startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), GPS_REQUEST_CODE)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
                Toast.makeText(requireContext(), "GPS es necesario para obtener la ubicación", Toast.LENGTH_SHORT).show()
            }
        val alert = dialogBuilder.create()
        alert.show()
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
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    checkGPSEnabled()
                } else {
                    Toast.makeText(requireContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GPS_REQUEST_CODE) {
            checkGPSEnabled()
        }
    }

    fun getValues(): Map<String, Any?> {
        return mapOf(
            "etFecha" to etFecha.text.toString(),
            "etHora" to etHora.text.toString(),
            "etSemana" to etSemana.text.toString().toIntOrNull(),
            "tvEvaluador" to tvEvaluador.text.toString(),
            "spinnerPolinizador" to (operarios.getOrNull(spinnerPolinizador.selectedItemPosition)?.second?.id),
            "etLote" to etLote.text.toString().toIntOrNull(),
            "etSeccion" to etSeccion.text.toString().toIntOrNull(),
            "etPalma" to etPalma.text.toString().toIntOrNull(),
            "etUbicacion" to etUbicacion.text.toString()
        )
    }
}