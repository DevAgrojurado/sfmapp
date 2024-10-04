package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ToggleButton
import com.agrojurado.sfmappv2.R
import com.google.android.material.textfield.TextInputEditText

class EvaluacionFragment : Fragment() {

    private lateinit var toggleEspateBueno: ToggleButton
    private lateinit var toggleEspateMalo: ToggleButton
    private lateinit var toggleAplicacionBueno: ToggleButton
    private lateinit var toggleAplicacionMalo: ToggleButton
    private lateinit var toggleMarcacionBueno: ToggleButton
    private lateinit var toggleMarcacionMalo: ToggleButton
    private lateinit var etMarcacion: TextInputEditText
    private lateinit var etRepaso1: TextInputEditText
    private lateinit var etRepaso2: TextInputEditText
    private lateinit var etObservaciones: TextInputEditText

    private var espateValue = -1
    private var aplicacionValue = -1
    private var marcacionValue = -1

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
            etRepaso1 = view.findViewById(R.id.etRepaso1)
            etRepaso2 = view.findViewById(R.id.etRepaso2)
            etObservaciones = view.findViewById(R.id.etObservaciones)
            toggleEspateBueno = view.findViewById(R.id.toggleEspateBueno)
            toggleEspateMalo = view.findViewById(R.id.toggleEspateMalo)
            toggleAplicacionBueno = view.findViewById(R.id.toggleAplicacionBueno)
            toggleAplicacionMalo = view.findViewById(R.id.toggleAplicacionMalo)
            toggleMarcacionBueno = view.findViewById(R.id.toggleMarcacionBueno)
            toggleMarcacionMalo = view.findViewById(R.id.toggleMarcacionMalo)

    }

    private fun setupListeners(){
        setupToggleButtons(toggleEspateBueno, toggleEspateMalo) { espateValue = it }
        setupToggleButtons(toggleAplicacionBueno, toggleAplicacionMalo) { aplicacionValue = it }
        setupToggleButtons(toggleMarcacionBueno, toggleMarcacionMalo) { marcacionValue = it }
    }

    private fun setupToggleButtons(toggleBueno: ToggleButton, toggleMalo: ToggleButton, updateValue: (Int) -> Unit) {
        toggleBueno.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                toggleMalo.isChecked = false
                updateValue(0)
            } else if (!toggleMalo.isChecked) {
                updateValue(-1)
            }
        }

        toggleMalo.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                toggleBueno.isChecked = false
                updateValue(1)
            } else if (!toggleBueno.isChecked) {
                updateValue(-1)
            }
        }
    }

    fun getValues(): Map<String, Any> {
        return mapOf(

            "espate" to espateValue,
            "aplicacion" to aplicacionValue,
            "marcacion" to marcacionValue
        )
    }
}