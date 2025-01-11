package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.dialogdetail

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion

class EvaluacionDetalleDialog(
    private val evaluacion: EvaluacionPolinizacion,
    private val nombrePolinizador: String,
    private val nombreEvaluador: String,
    private val descripcionLote: String,
) : DialogFragment() {

    private lateinit var recyclerView: RecyclerView

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            resources.getDimensionPixelSize(R.dimen.dialog_height)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_evaluacion_detalle, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar botón de cierre
        view.findViewById<ImageButton>(R.id.btnClose).setOnClickListener {
            dismiss()
        }

        recyclerView = view.findViewById(R.id.rvDetalleEvaluacion)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

        val items = listOf(
            "Fecha:" to (evaluacion.fecha ?: ""),
            "Hora:" to (evaluacion.hora ?: ""),
            "Semana:" to evaluacion.semana.toString(),
            "Ubicación:" to (evaluacion.ubicacion ?: ""),
            "Evaluador:" to nombreEvaluador,
            "Polinizador:" to nombrePolinizador,
            "Lote:" to descripcionLote,
            "Sección:" to (evaluacion.seccion.toString()),
            "Palma:" to (evaluacion.palma?.toString() ?: ""),
            "Inflorescencia:" to (evaluacion.inflorescencia?.toString() ?: ""),
            "Antesis:" to (evaluacion.antesis?.toString() ?: ""),
            "Post Antesis:" to (evaluacion.postAntesis?.toString() ?: ""),
            "Antesis Dejadas:" to (evaluacion.antesisDejadas?.toString() ?: ""),
            "Post Antesis Dejadas:" to (evaluacion.postAntesisDejadas?.toString() ?: ""),
            "Espate:" to (evaluacion.espate?.toString() ?: ""),
            "Aplicación:" to (evaluacion.aplicacion?.toString() ?: ""),
            "Marcación:" to (evaluacion.marcacion?.toString() ?: ""),
            "Repaso 1:" to (evaluacion.repaso1?.toString() ?: ""),
            "Repaso 2:" to (evaluacion.repaso2?.toString() ?: ""),
            "Observaciones:" to (evaluacion.observaciones ?: "")
        )

        recyclerView.adapter = DetalleEvaluacionAdapter(items)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setTitle("Detalles de la Evaluación")
        return dialog
    }
}