package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.Operario
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

    private val viewModel: EvaluacionViewModel by activityViewModels()
    private var operarios: List<Pair<String, Operario>> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_informacion_general, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rgInflorescencia = view.findViewById(R.id.rgInflorescencia)
        etFecha = view.findViewById(R.id.etFecha)
        etHora = view.findViewById(R.id.etHora)
        etSemana = view.findViewById(R.id.etSemana)
        tvEvaluador = view.findViewById(R.id.tvEvaluador)
        spinnerPolinizador = view.findViewById(R.id.spinnerPolinizador)
        etLote = view.findViewById(R.id.etLote)
        etSeccion = view.findViewById(R.id.etSeccion)

        // Initialize date and time
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        etFecha.setText(currentDate)
        etHora.setText(currentTime)

        // Observe logged-in user
        viewModel.loggedInUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                tvEvaluador.text = "${it.codigo} - ${it.nombre}"
                Log.d("InformacionGeneralFragment", "Usuario mostrado: ${it.codigo} - ${it.nombre}")
            } ?: run {
                Log.d("InformacionGeneralFragment", "No se pudo mostrar el usuario")
                tvEvaluador.text = "Usuario no disponible"
            }
        }

        // Observe operarios from ViewModel
        viewModel.operarios.observe(viewLifecycleOwner) { operariosList ->
            operarios = operariosList
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, operariosList.map { it.first })
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerPolinizador.adapter = adapter
        }

        // Set up inflorescencia radio group listener
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
            Log.d("InformacionGeneralFragment", "Inflorescencia seleccionada: $selectedInflorescencia")
        }

        // Load operarios
        viewModel.loadOperarios()
    }

    fun getValues(): Map<String, Any> {
        return mapOf(
            "etFecha" to etFecha.text.toString().ifEmpty { throw IllegalArgumentException("La fecha no puede estar vacía") },
            "etHora" to etHora.text.toString().ifEmpty { throw IllegalArgumentException("La hora no puede estar vacía") },
            "etSemana" to (etSemana.text.toString().toIntOrNull() ?: throw IllegalArgumentException("La semana debe ser un número")),
            "tvEvaluador" to tvEvaluador.text.toString().ifEmpty { throw IllegalArgumentException("El evaluador no puede estar vacío") },
            "spinnerPolinizador" to operarios[spinnerPolinizador.selectedItemPosition].second.id,
            "etLote" to (etLote.text.toString().toIntOrNull() ?: throw IllegalArgumentException("El lote debe ser un número")),
            "etSeccion" to (etSeccion.text.toString().toIntOrNull() ?: throw IllegalArgumentException("La sección debe ser un número")),

        )
    }
}