package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluacionfragmentsform

import android.os.Bundle
import android.util.Log
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
        Log.d("EvaluacionFragment", "onCreateView")
        _binding = FragmentEvaluacionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("espate", espateValue ?: -1)
        outState.putInt("aplicacion", aplicacionValue ?: -1)
        outState.putInt("marcacion", marcacionValue ?: -1)
        outState.putInt("repaso1", repaso1Value ?: -1)
        outState.putInt("repaso2", repaso2Value ?: -1)
        outState.putString("observaciones", binding.etObservaciones.text.toString())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        savedInstanceState?.let {
            if (_binding != null) {
                espateValue = it.getInt("espate", -1).takeIf { v -> v != -1 }
                aplicacionValue = it.getInt("aplicacion", -1).takeIf { v -> v != -1 }
                marcacionValue = it.getInt("marcacion", -1).takeIf { v -> v != -1 }
                repaso1Value = it.getInt("repaso1", -1).takeIf { v -> v != -1 }
                repaso2Value = it.getInt("repaso2", -1).takeIf { v -> v != -1 }
                binding.etObservaciones.setText(it.getString("observaciones", ""))
                espateValue?.let { v -> binding.toggleEspateBueno.isChecked = v == 0; binding.toggleEspateMalo.isChecked = v == 1 }
                aplicacionValue?.let { v -> binding.toggleAplicacionBueno.isChecked = v == 0; binding.toggleAplicacionMalo.isChecked = v == 1 }
                marcacionValue?.let { v -> binding.toggleMarcacionBueno.isChecked = v == 0; binding.toggleMarcacionMalo.isChecked = v == 1 }
                repaso1Value?.let { v -> binding.toggleRepaso1Bueno.isChecked = v == 0; binding.toggleRepaso1Malo.isChecked = v == 1 }
                repaso2Value?.let { v -> binding.toggleRepaso2Bueno.isChecked = v == 0; binding.toggleRepaso2Malo.isChecked = v == 1 }
            }
        }
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
            try {
                if (isChecked) {
                    toggleMalo.isChecked = false
                    updateValue(0)
                } else if (!toggleMalo.isChecked) {
                    updateValue(null)
                }
            } catch (e: Exception) {
                Log.e("EvaluacionFragment", "Error in toggleBueno listener: ${e.message}", e)
            }
        }

        toggleMalo.setOnCheckedChangeListener { _, isChecked ->
            try {
                if (isChecked) {
                    toggleBueno.isChecked = false
                    updateValue(1)
                } else if (!toggleBueno.isChecked) {
                    updateValue(null)
                }
            } catch (e: Exception) {
                Log.e("EvaluacionFragment", "Error in toggleMalo listener: ${e.message}", e)
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
        Log.d("EvaluacionFragment", "onDestroyView")
        _binding = null
    }
}