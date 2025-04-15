package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion

import android.content.Context
import android.graphics.Color
import android.view.Window
import android.widget.ImageButton
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion

object EvaluacionTableDialog {

    fun showEvaluationTableDialog(
        context: Context,
        evaluaciones: List<EvaluacionPolinizacion>,
        evaluadorMap: Map<Int, String>,
        loteMap: Map<Int, String>
    ) {
        val dialog = android.app.Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_evaluation_details)

        val tableLayout = dialog.findViewById<TableLayout>(R.id.tableLayout)
        dialog.findViewById<ImageButton>(R.id.btnClose).setOnClickListener { dialog.dismiss() }

        // Encabezados de la tabla
        val headerRow = TableRow(context).apply {
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(Color.parseColor("#D3D3D3"))
        }
        listOf(
            "Fecha", "Hora", "Semana", "Ubicación", "Evaluador", "Lote", "Sección", "Palma",
            "Inflorescencia", "Antesis", "Post Antesis", "Antesis Dejadas", "Post Antesis Dejadas",
            "Espate", "Aplicación", "Marcación", "Repaso 1", "Repaso 2", "Observaciones"
        ).forEach { headerText ->
            TextView(context).apply {
                text = headerText
                setPadding(8, 8, 8, 8)
                setTextColor(Color.BLACK)
                setTypeface(null, android.graphics.Typeface.BOLD)
            }.also { headerRow.addView(it) }
        }
        tableLayout.addView(headerRow)

        // Filas de datos
        evaluaciones.forEachIndexed { index, evaluacion ->
            val row = TableRow(context).apply {
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT
                )
                setBackgroundColor(if (index % 2 == 0) Color.WHITE else Color.parseColor("#F5F5F5"))
            }

            listOf(
                evaluacion.fecha ?: "",
                evaluacion.hora ?: "",
                evaluacion.semana.toString(),
                evaluacion.ubicacion ?: "",
                evaluadorMap[evaluacion.idEvaluador] ?: "",
                loteMap[evaluacion.idlote] ?: "",
                evaluacion.seccion.toString(),
                evaluacion.palma?.toString() ?: "",
                evaluacion.inflorescencia?.toString() ?: "",
                evaluacion.antesis?.toString() ?: "",
                evaluacion.postAntesis?.toString() ?: "",
                evaluacion.antesisDejadas?.toString() ?: "",
                evaluacion.postAntesisDejadas?.toString() ?: "",
                evaluacion.espate?.toString() ?: "",
                evaluacion.aplicacion?.toString() ?: "",
                evaluacion.marcacion?.toString() ?: "",
                evaluacion.repaso1?.toString() ?: "",
                evaluacion.repaso2?.toString() ?: "",
                evaluacion.observaciones ?: ""
            ).forEach { text ->
                TextView(context).apply {
                    this.text = text
                    setPadding(8, 8, 8, 8)
                }.also { row.addView(it) }
            }

            tableLayout.addView(row)
        }

        dialog.window?.apply {
            val horizontalMargin = context.resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)
            val verticalMargin = context.resources.getDimensionPixelSize(R.dimen.activity_vertical_margin)
            val dialogHeight = context.resources.getDimensionPixelSize(R.dimen.dialog_height)
            val displayMetrics = context.resources.displayMetrics
            setLayout(
                displayMetrics.widthPixels - (horizontalMargin * 2),
                minOf(dialogHeight, displayMetrics.heightPixels - (verticalMargin * 2))
            )
            setGravity(android.view.Gravity.CENTER)
        }
        dialog.show()
    }
}