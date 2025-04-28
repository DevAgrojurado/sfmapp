package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluacionfragmentsform

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.agrojurado.sfmappv2.R
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
        Log.d("EvaluacionFragment", "onViewCreated")
        
        if (savedInstanceState != null) {
            Log.d("EvaluacionFragment", "Restaurando estado...")
            restoreInstanceState(savedInstanceState)
        } else {
            // Opcional: Establecer un estado inicial si no se restaura nada
            // Por ejemplo, si quieres que empiecen sin seleccionar nada (si quitaste selectionRequired)
             // resetInitialState()
        }

        setupListeners()
    }

    private fun restoreInstanceState(savedInstanceState: Bundle) {
         if (_binding == null) return // Comprobación de seguridad
         
         binding.etObservaciones.setText(savedInstanceState.getString("observaciones", ""))
         
         // Restaurar selección (Bueno=0, Malo=1)
         espateValue = savedInstanceState.getInt("espate", -1).takeIf { it != -1 }
         espateValue?.let { binding.toggleGroupEspate.check(if (it == 0) R.id.btnEspateBueno else R.id.btnEspateMalo) }
         
         aplicacionValue = savedInstanceState.getInt("aplicacion", -1).takeIf { it != -1 }
         aplicacionValue?.let { binding.toggleGroupAplicacion.check(if (it == 0) R.id.btnAplicacionBueno else R.id.btnAplicacionMalo) }

         marcacionValue = savedInstanceState.getInt("marcacion", -1).takeIf { it != -1 }
         marcacionValue?.let { binding.toggleGroupMarcacion.check(if (it == 0) R.id.btnMarcacionBueno else R.id.btnMarcacionMalo) }

         // Restaurar selección INVERSA (Bueno=1, Malo=0)
         repaso1Value = savedInstanceState.getInt("repaso1", -1).takeIf { it != -1 }
         repaso1Value?.let { binding.toggleGroupRepaso1.check(if (it == 1) R.id.btnRepaso1Bueno else R.id.btnRepaso1Malo) }

         repaso2Value = savedInstanceState.getInt("repaso2", -1).takeIf { it != -1 }
         repaso2Value?.let { binding.toggleGroupRepaso2.check(if (it == 1) R.id.btnRepaso2Bueno else R.id.btnRepaso2Malo) }

         Log.d("EvaluacionFragment", "Estado restaurado: espate=$espateValue, repaso1=$repaso1Value")
    }

    private fun setupListeners() {
        if (_binding == null) {
            Log.e("EvaluacionFragment", "Binding es nulo en setupListeners")
            return
        }
        Log.d("EvaluacionFragment", "Configurando listeners")
        
        // Grupos con lógica Bueno=0, Malo=1
        binding.toggleGroupEspate.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) { 
                espateValue = when (checkedId) {
                    R.id.btnEspateBueno -> 0
                    R.id.btnEspateMalo -> 1
                    else -> null
                }
                Log.d("EvaluacionFragment", "Espate checked: $checkedId -> value: $espateValue")
            } else if (group.checkedButtonId == View.NO_ID) {
                 espateValue = null
                 Log.d("EvaluacionFragment", "Espate cleared")
            }
        }

        binding.toggleGroupAplicacion.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                aplicacionValue = if (checkedId == R.id.btnAplicacionBueno) 0 else 1
                Log.d("EvaluacionFragment", "Aplicacion checked: $checkedId -> value: $aplicacionValue")
            } else if (group.checkedButtonId == View.NO_ID) {
                aplicacionValue = null
            }
        }

        binding.toggleGroupMarcacion.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                marcacionValue = if (checkedId == R.id.btnMarcacionBueno) 0 else 1
                Log.d("EvaluacionFragment", "Marcacion checked: $checkedId -> value: $marcacionValue")
            } else if (group.checkedButtonId == View.NO_ID) {
                marcacionValue = null
            }
        }

        // Grupos con lógica INVERSA: Bueno=1, Malo=0
        binding.toggleGroupRepaso1.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                repaso1Value = if (checkedId == R.id.btnRepaso1Bueno) 1 else 0 // Lógica invertida
                 Log.d("EvaluacionFragment", "Repaso1 checked: $checkedId -> value: $repaso1Value")
           } else if (group.checkedButtonId == View.NO_ID) {
                repaso1Value = null
                 Log.d("EvaluacionFragment", "Repaso1 cleared")
            }
        }

        binding.toggleGroupRepaso2.addOnButtonCheckedListener { group, checkedId, isChecked ->
             if (isChecked) {
                repaso2Value = if (checkedId == R.id.btnRepaso2Bueno) 1 else 0 // Lógica invertida
                Log.d("EvaluacionFragment", "Repaso2 checked: $checkedId -> value: $repaso2Value")
            } else if (group.checkedButtonId == View.NO_ID) {
                repaso2Value = null
                 Log.d("EvaluacionFragment", "Repaso2 cleared")
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
            "observaciones" to binding.etObservaciones.text.toString()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("EvaluacionFragment", "onDestroyView")
        _binding = null
    }
}