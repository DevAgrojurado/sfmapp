package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import com.google.android.material.textfield.TextInputEditText
import androidx.fragment.app.Fragment
import com.agrojurado.sfmappv2.R

class DetallesPolinizacionFragment : Fragment() {

    private lateinit var etAntesis: TextInputEditText
    private lateinit var etPostAntesis: TextInputEditText
    private lateinit var btnAntesisMinus: ImageButton
    private lateinit var btnAntesisPlus: ImageButton
    private lateinit var btnPostAntesisMinus: ImageButton
    private lateinit var btnPostAntesisPlus: ImageButton
    private lateinit var etAntesisDejadas: TextInputEditText
    private lateinit var etPostAntesisDejadas: TextInputEditText
    private lateinit var btnAntesisDejadasMinus: ImageButton
    private lateinit var btnAntesisDejadasPlus: ImageButton
    private lateinit var btnPostAntesisDejadasMinus: ImageButton
    private lateinit var btnPostAntesisDejadasPlus: ImageButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_detalles_polinizacion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupListeners()
    }

    private fun initializeViews(view: View) {
        etAntesis = view.findViewById(R.id.etAntesis)
        etPostAntesis = view.findViewById(R.id.etPostAntesis)
        btnAntesisMinus = view.findViewById(R.id.btnAntesisMinus)
        btnAntesisPlus = view.findViewById(R.id.btnAntesisPlus)
        btnPostAntesisMinus = view.findViewById(R.id.btnPostAntesisMinus)
        btnPostAntesisPlus = view.findViewById(R.id.btnPostAntesisPlus)
        etAntesisDejadas = view.findViewById(R.id.etAntesisDejadas)
        etPostAntesisDejadas = view.findViewById(R.id.etPostAntesisDejadas)
        btnAntesisDejadasMinus = view.findViewById(R.id.btnAntesisDejadasMinus)
        btnAntesisDejadasPlus = view.findViewById(R.id.btnAntesisDejadasPlus)
        btnPostAntesisDejadasMinus = view.findViewById(R.id.btnPostAntesisDejadasMinus)
        btnPostAntesisDejadasPlus = view.findViewById(R.id.btnPostAntesisDejadasPlus)
    }

    private fun setupListeners() {
        btnAntesisMinus.setOnClickListener { updateCount(etAntesis, false) }
        btnAntesisPlus.setOnClickListener { updateCount(etAntesis, true) }
        btnPostAntesisMinus.setOnClickListener { updateCount(etPostAntesis, false) }
        btnPostAntesisPlus.setOnClickListener { updateCount(etPostAntesis, true) }
        btnAntesisDejadasMinus.setOnClickListener { updateCount(etAntesisDejadas, false) }
        btnAntesisDejadasPlus.setOnClickListener { updateCount(etAntesisDejadas, true) }
        btnPostAntesisDejadasMinus.setOnClickListener { updateCount(etPostAntesisDejadas, false) }
        btnPostAntesisDejadasPlus.setOnClickListener { updateCount(etPostAntesisDejadas, true) }
    }

    private fun updateCount(editText: TextInputEditText, isIncrement: Boolean) {
        var count = editText.text.toString().toIntOrNull() ?: 0
        if (isIncrement) count++ else if (count > 0) count--
        editText.setText(count.toString())
    }

    fun getValues(): Map<String, Any> {
        return mapOf(
            "antesis" to (etAntesis.text.toString().toIntOrNull() ?: 0),
            "postAntesis" to (etPostAntesis.text.toString().toIntOrNull() ?: 0),
            "antesisDejadas" to (etAntesisDejadas.text.toString().toIntOrNull() ?: 0),
            "postAntesisDejadas" to (etPostAntesisDejadas.text.toString().toIntOrNull() ?: 0)
        )
    }
}