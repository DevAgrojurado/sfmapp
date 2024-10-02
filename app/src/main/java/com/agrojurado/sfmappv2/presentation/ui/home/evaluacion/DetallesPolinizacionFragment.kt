package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.agrojurado.sfmappv2.R
import com.google.android.material.textfield.TextInputEditText

class DetallesPolinizacionFragment : Fragment() {

    private lateinit var etInflorescencia: TextInputEditText
    private lateinit var etAntesis: TextInputEditText
    private lateinit var etPostAntesis: TextInputEditText
    private lateinit var etEspate: TextInputEditText
    private lateinit var etAplicacion: TextInputEditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_detalles_polinizacion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etInflorescencia = view.findViewById(R.id.etInflorescencia)
        etAntesis = view.findViewById(R.id.etAntesis)
        etPostAntesis = view.findViewById(R.id.etPostAntesis)
        etEspate = view.findViewById(R.id.etEspate)
        etAplicacion = view.findViewById(R.id.etAplicacion)

    }
}