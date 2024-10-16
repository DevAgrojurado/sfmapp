package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion

class EvaluacionDetalleDialog(
    private val evaluacion: EvaluacionPolinizacion,
    private val nombrePolinizador: String,
    private val nombreEvaluador: String
) : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_evaluacion_detalle, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvFecha).text = "Fecha: ${evaluacion.fecha}"
        view.findViewById<TextView>(R.id.tvHora).text = "Hora: ${evaluacion.hora}"
        view.findViewById<TextView>(R.id.tvSemana).text = "Semana: ${evaluacion.semana}"
        view.findViewById<TextView>(R.id.tvUbicacion).text = "Ubicación: ${evaluacion.ubicacion}"
        view.findViewById<TextView>(R.id.tvEvaluador).text = "Evaluador: $nombreEvaluador"
        view.findViewById<TextView>(R.id.tvPolinizador).text = "Polinizador: $nombrePolinizador"
        view.findViewById<TextView>(R.id.tvLote).text = "Lote: ${evaluacion.lote}"
        view.findViewById<TextView>(R.id.tvSeccion).text = "Sección: ${evaluacion.seccion}"
        view.findViewById<TextView>(R.id.tvPalma).text = "Palma: ${evaluacion.palma}"
        view.findViewById<TextView>(R.id.tvInflorescencia).text = "Inflorescencia: ${evaluacion.inflorescencia}"
        view.findViewById<TextView>(R.id.tvAntesis).text = "Antesis: ${evaluacion.antesis}"
        view.findViewById<TextView>(R.id.tvPostAntesis).text = "Post Antesis: ${evaluacion.postAntesis}"
        view.findViewById<TextView>(R.id.tvAntesisDejadas).text = "Antesis Dejadas: ${evaluacion.antesisDejadas}"
        view.findViewById<TextView>(R.id.tvPostAntesisDejadas).text = "Post Antesis Dejadas: ${evaluacion.postAntesisDejadas}"
        view.findViewById<TextView>(R.id.tvEspate).text = "Espate: ${evaluacion.espate}"
        view.findViewById<TextView>(R.id.tvAplicacion).text = "Aplicación: ${evaluacion.aplicacion}"
        view.findViewById<TextView>(R.id.tvMarcacion).text = "Marcación: ${evaluacion.marcacion}"
        view.findViewById<TextView>(R.id.tvRepaso1).text = "Repaso 1: ${evaluacion.repaso1}"
        view.findViewById<TextView>(R.id.tvRepaso2).text = "Repaso 2: ${evaluacion.repaso2}"
        view.findViewById<TextView>(R.id.tvObservaciones).text = "Observaciones: ${evaluacion.observaciones}"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setTitle("Detalles de la Evaluación")
        return dialog
    }
}