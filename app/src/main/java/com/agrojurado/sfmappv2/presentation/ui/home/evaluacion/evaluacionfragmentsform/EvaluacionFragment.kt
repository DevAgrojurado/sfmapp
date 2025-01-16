package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluacionfragmentsform

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ToggleButton
import com.agrojurado.sfmappv2.R
import com.google.android.material.textfield.TextInputEditText

class EvaluacionFragment : Fragment() {

    private lateinit var etMarcacion: TextInputEditText
    private lateinit var toggleEspateBueno: ToggleButton
    private lateinit var toggleEspateMalo: ToggleButton
    private lateinit var toggleAplicacionBueno: ToggleButton
    private lateinit var toggleAplicacionMalo: ToggleButton
    private lateinit var toggleMarcacionBueno: ToggleButton
    private lateinit var toggleMarcacionMalo: ToggleButton
    private lateinit var toggleRepaso1Bueno: ToggleButton
    private lateinit var toggleRepaso1Malo: ToggleButton
    private lateinit var toggleRepaso2Bueno: ToggleButton
    private lateinit var toggleRepaso2Malo: ToggleButton
    private lateinit var etObservaciones: TextInputEditText
    private lateinit var btnSave: Button

    private var espateValue: Int? = null
    private var aplicacionValue: Int? = null
    private var marcacionValue: Int? = null
    private var repaso1Value: Int? = null
    private var repaso2Value: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_evaluacion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupListeners()

    }

    private fun initializeViews(view: View) {
        etMarcacion = view.findViewById(R.id.etMarcacion)
        etObservaciones = view.findViewById(R.id.etObservaciones)
        toggleEspateBueno = view.findViewById(R.id.toggleEspateBueno)
        toggleEspateMalo = view.findViewById(R.id.toggleEspateMalo)
        toggleAplicacionBueno = view.findViewById(R.id.toggleAplicacionBueno)
        toggleAplicacionMalo = view.findViewById(R.id.toggleAplicacionMalo)
        toggleMarcacionBueno = view.findViewById(R.id.toggleMarcacionBueno)
        toggleMarcacionMalo = view.findViewById(R.id.toggleMarcacionMalo)
        toggleRepaso1Bueno = view.findViewById(R.id.toggleRepaso1Bueno)
        toggleRepaso1Malo = view.findViewById(R.id.toggleRepaso1Malo)
        toggleRepaso2Bueno = view.findViewById(R.id.toggleRepaso2Bueno)
        toggleRepaso2Malo = view.findViewById(R.id.toggleRepaso2Malo)
    }

    private fun setupListeners() {
        setupToggleButtons(toggleEspateBueno, toggleEspateMalo) { espateValue = it }
        setupToggleButtons(toggleAplicacionBueno, toggleAplicacionMalo) { aplicacionValue = it }
        setupToggleButtons(toggleMarcacionBueno, toggleMarcacionMalo) { marcacionValue = it }
        setupToggleButtons(toggleRepaso1Bueno, toggleRepaso1Malo) { repaso1Value = it?.let { if (it == 0) 1 else 0 } }
        setupToggleButtons(toggleRepaso2Bueno, toggleRepaso2Malo) { repaso2Value = it?.let { if (it == 0) 1 else 0 } }
    }

    private fun setupToggleButtons(toggleBueno: ToggleButton, toggleMalo: ToggleButton, updateValue: (Int?) -> Unit) {
        toggleBueno.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                toggleMalo.isChecked = false
                updateValue(0)
            } else if (!toggleMalo.isChecked) {
                updateValue(null)
            }
        }

        toggleMalo.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                toggleBueno.isChecked = false
                updateValue(1)
            } else if (!toggleBueno.isChecked) {
                updateValue(null)
            }
        }
    }

    fun getValues(): Map<String, Any?> {
        return mapOf(
            "espate" to espateValue,
            "aplicacion" to aplicacionValue,
            "marcacion" to marcacionValue,
            "repaso1" to repaso1Value,
            "repaso2" to repaso2Value,
            "observaciones" to etObservaciones.text.toString()
        )
    }
}