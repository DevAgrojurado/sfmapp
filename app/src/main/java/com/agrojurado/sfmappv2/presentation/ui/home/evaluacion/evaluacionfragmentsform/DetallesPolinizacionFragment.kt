package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluacionfragmentsform

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.agrojurado.sfmappv2.databinding.FragmentDetallesPolinizacionBinding
import com.google.android.material.textfield.TextInputEditText

class DetallesPolinizacionFragment : Fragment() {

    private var _binding: FragmentDetallesPolinizacionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("DetallesPolinizacionFragment", "onCreateView")
        _binding = FragmentDetallesPolinizacionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("antesis", binding.etAntesis.text.toString())
        outState.putString("postAntesis", binding.etPostAntesis.text.toString())
        outState.putString("antesisDejadas", binding.etAntesisDejadas.text.toString())
        outState.putString("postAntesisDejadas", binding.etPostAntesisDejadas.text.toString())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        savedInstanceState?.let {
            if (_binding != null) {
                binding.etAntesis.setText(it.getString("antesis", "0"))
                binding.etPostAntesis.setText(it.getString("postAntesis", "0"))
                binding.etAntesisDejadas.setText(it.getString("antesisDejadas", "0"))
                binding.etPostAntesisDejadas.setText(it.getString("postAntesisDejadas", "0"))
            }
        }
    }

    private fun setupListeners() {
        with(binding) {
            btnAntesisMinus.setOnClickListener { updateCount(etAntesis, false) }
            btnAntesisPlus.setOnClickListener { updateCount(etAntesis, true) }
            btnPostAntesisMinus.setOnClickListener { updateCount(etPostAntesis, false) }
            btnPostAntesisPlus.setOnClickListener { updateCount(etPostAntesis, true) }
            btnAntesisDejadasMinus.setOnClickListener { updateCount(etAntesisDejadas, false) }
            btnAntesisDejadasPlus.setOnClickListener { updateCount(etAntesisDejadas, true) }
            btnPostAntesisDejadasMinus.setOnClickListener { updateCount(etPostAntesisDejadas, false) }
            btnPostAntesisDejadasPlus.setOnClickListener { updateCount(etPostAntesisDejadas, true) }
        }
    }

    private fun updateCount(editText: TextInputEditText, isIncrement: Boolean) {
        try {
            var count = editText.text.toString().toIntOrNull() ?: 0
            if (isIncrement) count++ else if (count > 0) count--
            editText.setText(count.toString())
        } catch (e: Exception) {
            Log.e("DetallesPolinizacionFragment", "Error in updateCount: ${e.message}", e)
        }
    }

    fun getValues(): Map<String, Any?> {
        return with(binding) {
            mapOf(
                "antesis" to etAntesis.text.toString().toIntOrNull(),
                "postAntesis" to etPostAntesis.text.toString().toIntOrNull(),
                "antesisDejadas" to etAntesisDejadas.text.toString().toIntOrNull(),
                "postAntesisDejadas" to etPostAntesisDejadas.text.toString().toIntOrNull()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("DetallesPolinizacionFragment", "onDestroyView")
        _binding = null
    }
}