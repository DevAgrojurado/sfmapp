package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.agrojurado.sfmappv2.R
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class InformacionGeneralFragment : Fragment() {
    private lateinit var etFecha: TextInputEditText
    private lateinit var etHora: TextInputEditText
    private lateinit var etSemana: TextInputEditText
    private lateinit var etIdEvaluador: TextInputEditText
    private lateinit var etCodigoEvaluador: TextInputEditText
    private lateinit var etLote: TextInputEditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_informacion_general, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etFecha = view.findViewById(R.id.etFecha)
        etHora = view.findViewById(R.id.etHora)
        etSemana = view.findViewById(R.id.etSemana)
        etIdEvaluador = view.findViewById(R.id.etIdEvaluador)
        etCodigoEvaluador = view.findViewById(R.id.etCodigoEvaluador)
        etLote = view.findViewById((R.id.etLote))

        // Inicializar fecha y hora actuales
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        etFecha.setText(currentDate)
        etHora.setText(currentTime)
    }
}