package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.agrojurado.sfmappv2.R
import com.google.android.material.textfield.TextInputEditText

class EvaluacionFragment : Fragment() {

    private lateinit var etMarcacion: TextInputEditText
    private lateinit var etRepaso1: TextInputEditText
    private lateinit var etRepaso2: TextInputEditText
    private lateinit var etObservaciones: TextInputEditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_evaluacion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etMarcacion = view.findViewById(R.id.etMarcacion)
        etRepaso1 = view.findViewById(R.id.etRepaso1)
        etRepaso2 = view.findViewById(R.id.etRepaso2)
        etObservaciones = view.findViewById(R.id.etObservaciones)

    }
}