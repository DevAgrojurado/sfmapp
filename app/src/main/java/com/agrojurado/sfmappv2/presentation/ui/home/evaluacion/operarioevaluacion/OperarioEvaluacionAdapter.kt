package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.operarioevaluacion

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import com.google.android.material.button.MaterialButton

class OperarioEvaluacionAdapter(
    private var evaluacionesPorPolinizador: Map<Pair<Int, String>, List<EvaluacionPolinizacion>> = emptyMap(),
    private val onItemClick: (Int, String) -> Unit,
    private val onExportPdfClick: (List<EvaluacionPolinizacion>, String) -> Unit,
    private val onExportExcelClick: (List<EvaluacionPolinizacion>, String) -> Unit,
    private val countUniquePalms: (List<EvaluacionPolinizacion>) -> Int,
    private val getEvaluadorMap: () -> Map<Int, String>,
    private val getLoteMap: () -> Map<Int, String>
) : RecyclerView.Adapter<OperarioEvaluacionAdapter.ViewHolder>() {

    fun updateItems(newItems: Map<Pair<Int, String>, List<EvaluacionPolinizacion>>) {
        evaluacionesPorPolinizador = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_operario_evaluacion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val polinizador = evaluacionesPorPolinizador.keys.toList()[position]
        val evaluaciones = evaluacionesPorPolinizador[polinizador] ?: emptyList()

        // Calcular estadísticas
        val totalPalmas = countUniquePalms(evaluaciones)
        val totalEventos = evaluaciones.sumOf { it.inflorescencia ?: 0 }

        holder.bind(
            nombrePolinizador = polinizador.second,
            totalPalmas = totalPalmas,
            totalEventos = totalEventos
        )

        // Click normal
        holder.itemView.setOnClickListener {
            onItemClick(polinizador.first, polinizador.second)
        }

        // Click largo para mostrar detalles
        holder.itemView.setOnLongClickListener {
            showDetailDialog(holder.itemView.context, evaluaciones)
            true
        }

        // Click para exportar PDF
        //holder.btnExportarPdf.setOnClickListener { onExportPdfClick(evaluaciones, polinizador.second) }

        holder.btnExportExcel.setOnClickListener {
            onExportExcelClick(evaluaciones, polinizador.second) }
    }

    private fun showDetailDialog(context: Context, evaluaciones: List<EvaluacionPolinizacion>) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_evaluation_details)

        val tableLayout = dialog.findViewById<TableLayout>(R.id.tableLayout)


        // Configurar el botón de cierre
        dialog.findViewById<ImageButton>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        val evaluadorMap = getEvaluadorMap()
        val loteMap = getLoteMap()

        // Agregar filas de datos
        evaluaciones.forEachIndexed { index, evaluacion ->
            val row = TableRow(context)
            row.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )

            // Alternar colores de fondo para mejor legibilidad
            row.setBackgroundColor(if (index % 2 == 0) Color.WHITE else Color.parseColor("#F5F5F5"))

            // Agregar todas las columnas
            evaluacion.fecha?.let { addCell(row, it) }
            evaluacion.hora?.let { addCell(row, it) }
            addCell(row, evaluacion.semana.toString())
            evaluacion.ubicacion?.let { addCell(row, it) }
            addCell(row, evaluadorMap[evaluacion.idEvaluador] ?: "")
            addCell(row, loteMap[evaluacion.idlote] ?: "")
            addCell(row, evaluacion.seccion.toString())
            addCell(row, evaluacion.palma.toString())
            addCell(row, evaluacion.inflorescencia?.toString() ?: "")
            addCell(row, evaluacion.antesis?.toString() ?: "")
            addCell(row, evaluacion.postAntesis?.toString() ?: "")
            addCell(row, evaluacion.antesisDejadas?.toString() ?: "")
            addCell(row, evaluacion.postAntesisDejadas?.toString() ?: "")
            addCell(row, evaluacion.espate?.toString() ?: "")
            addCell(row, evaluacion.aplicacion?.toString() ?: "")
            addCell(row, evaluacion.marcacion?.toString() ?: "")
            addCell(row, evaluacion.repaso1?.toString() ?: "")
            addCell(row, evaluacion.repaso2?.toString() ?: "")
            evaluacion.observaciones?.let { addCell(row, it) }

            tableLayout.addView(row)
        }
        dialog.window?.apply {
            val horizontalMargin =
                context.resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)
            val verticalMargin =
                context.resources.getDimensionPixelSize(R.dimen.activity_vertical_margin)

            // Calcular el alto del diálogo considerando los márgenes
            val dialogHeight = context.resources.getDimensionPixelSize(R.dimen.dialog_height)
            val displayMetrics = context.resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels
            val screenWidth = displayMetrics.widthPixels

            // Asegurar que el diálogo no exceda el tamaño de la pantalla menos los márgenes
            val finalHeight = minOf(dialogHeight, screenHeight - (verticalMargin * 2))
            val finalWidth = screenWidth - (horizontalMargin * 2)

            setLayout(
                finalWidth,
                finalHeight
            )

            // Centrar el diálogo
            setGravity(android.view.Gravity.CENTER)

            // Establecer las animaciones y el fondo semitransparente

        }
        dialog.show()
    }

    private fun addCell(row: TableRow, text: String) {
        val textView = TextView(row.context).apply {
            this.text = text
            setPadding(8, 8, 8, 8)
        }
        row.addView(textView)
    }

    override fun getItemCount() = evaluacionesPorPolinizador.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNombrePolinizador: TextView = view.findViewById(R.id.tvPolinizadorNombre)
        private val tvTotalPalmas: TextView = view.findViewById(R.id.tvPalmas)
        private val tvTotalEventos: TextView = view.findViewById(R.id.tvEventos)
        //val btnExportarPdf: MaterialButton = view.findViewById(R.id.btnExportarPdf)
        val btnExportExcel: MaterialButton = itemView.findViewById(R.id.btnExportToExcel)

        @SuppressLint("SetTextI18n")
        fun bind(nombrePolinizador: String, totalPalmas: Int, totalEventos: Int) {
            tvNombrePolinizador.text = nombrePolinizador
            tvTotalPalmas.text = "Palmas: $totalPalmas"
            tvTotalEventos.text = "Eventos: $totalEventos"
        }
    }
}