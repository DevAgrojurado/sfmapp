package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluacionfragmentsform

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ToggleButton
import com.agrojurado.sfmappv2.databinding.FragmentEvaluacionBinding

class EvaluacionFragment : Fragment() {

    private var _binding: FragmentEvaluacionBinding? = null
    private val binding get() = _binding!!

    private var espateValue: Int? = null
    private var aplicacionValue: Int? = null
    private var marcacionValue: Int? = null
    private var repaso1Value: Int? = null
    private var repaso2Value: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEvaluacionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
    }

    private fun setupListeners() {
        with(binding) {
            setupToggleButtons(toggleEspateBueno, toggleEspateMalo) { espateValue = it }
            setupToggleButtons(toggleAplicacionBueno, toggleAplicacionMalo) { aplicacionValue = it }
            setupToggleButtons(toggleMarcacionBueno, toggleMarcacionMalo) { marcacionValue = it }
            setupToggleButtons(toggleRepaso1Bueno, toggleRepaso1Malo) { repaso1Value = it?.let { if (it == 0) 1 else 0 } }
            setupToggleButtons(toggleRepaso2Bueno, toggleRepaso2Malo) { repaso2Value = it?.let { if (it == 0) 1 else 0 } }
        }
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
        return with(binding) {
            mapOf(
                "espate" to espateValue,
                "aplicacion" to aplicacionValue,
                "marcacion" to marcacionValue,
                "repaso1" to repaso1Value,
                "repaso2" to repaso2Value,
                "observaciones" to etObservaciones.text.toString()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}