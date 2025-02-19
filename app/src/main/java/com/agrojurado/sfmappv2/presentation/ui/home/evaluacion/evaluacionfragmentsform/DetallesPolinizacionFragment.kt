package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluacionfragmentsform

import android.os.Bundle
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
        _binding = FragmentDetallesPolinizacionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
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
        var count = editText.text.toString().toIntOrNull() ?: 0
        if (isIncrement) count++ else if (count > 0) count--
        editText.setText(count.toString())
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
        _binding = null
    }
}