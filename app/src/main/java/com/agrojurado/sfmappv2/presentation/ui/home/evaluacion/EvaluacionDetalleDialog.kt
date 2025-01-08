package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.agrojurado.sfmappv2.databinding.DialogEvaluacionDetalleBinding
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion

class EvaluacionDetalleDialog(
    private val evaluacion: EvaluacionPolinizacion,
    private val nombrePolinizador: String,
    private val nombreEvaluador: String,
    private val descripcionLote: String,
) : DialogFragment() {

    private lateinit var binding: DialogEvaluacionDetalleBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Asegúrate de inflar correctamente el binding
        binding = DialogEvaluacionDetalleBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Usando el binding para acceder a las vistas
        binding.tvFecha.text = "Fecha: ${evaluacion.fecha}"
        binding.tvHora.text = "Hora: ${evaluacion.hora}"
        binding.tvSemana.text = "Semana: ${evaluacion.semana}"
        binding.tvUbicacion.text = "Ubicación: ${evaluacion.ubicacion}"
        binding.tvEvaluador.text = "Evaluador: $nombreEvaluador"
        binding.tvPolinizador.text = "Polinizador: $nombrePolinizador"
        binding.tvLote.text = "Lote: $descripcionLote"
        binding.tvSeccion.text = "Sección: ${evaluacion.seccion}"
        binding.tvPalma.text = "Palma: ${evaluacion.palma}"
        binding.tvInflorescencia.text = "Inflorescencia: ${evaluacion.inflorescencia}"
        binding.tvAntesis.text = "Antesis: ${evaluacion.antesis}"
        binding.tvPostAntesis.text = "Post Antesis: ${evaluacion.postAntesis}"
        binding.tvAntesisDejadas.text = "Antesis Dejadas: ${evaluacion.antesisDejadas}"
        binding.tvPostAntesisDejadas.text = "Post Antesis Dejadas: ${evaluacion.postAntesisDejadas}"
        binding.tvEspate.text = "Espate: ${evaluacion.espate}"
        binding.tvAplicacion.text = "Aplicación: ${evaluacion.aplicacion}"
        binding.tvMarcacion.text = "Marcación: ${evaluacion.marcacion}"
        binding.tvRepaso1.text = "Repaso 1: ${evaluacion.repaso1}"
        binding.tvRepaso2.text = "Repaso 2: ${evaluacion.repaso2}"
        binding.tvObservaciones.text = "Observaciones: ${evaluacion.observaciones}"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setTitle("Detalles de la Evaluación")
        return dialog
    }
}
