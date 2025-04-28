package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.operarioevaluacion

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.EvaluacionTableDialog
import com.agrojurado.sfmappv2.utils.ExcelUtils
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OperarioEvaluacionAdapter(
    private val semana: Int,
    private val onItemClick: (Int, String, Int?) -> Unit,
    private val countUniquePalms: (List<EvaluacionPolinizacion>, Int?) -> Int,
    private val getEvaluadorMap: () -> Map<Int, String>,
    private val getLoteMap: () -> Map<Int, String>,
    private val getPhotoUrl: suspend (Int, Int, Int) -> String?,
    private val fragmentReference: Fragment
) : RecyclerView.Adapter<OperarioEvaluacionAdapter.ViewHolder>() {

    private var evaluacionesPorPolinizadorYEvaluacion: Map<Triple<Int, String, Int>, List<EvaluacionPolinizacion>> = emptyMap()

    fun updateItems(newEvaluaciones: Map<Pair<Int, String>, List<EvaluacionPolinizacion>>) {
        val newItems = mutableMapOf<Triple<Int, String, Int>, List<EvaluacionPolinizacion>>()
        newEvaluaciones.forEach { (polinizador, evaluaciones) ->
            evaluaciones.groupBy { it.evaluacionGeneralId }.forEach { (evalGeneralId, evaluacionesDeEvalGeneral) ->
                if (evalGeneralId != null) {
                    val key = Triple(polinizador.first, polinizador.second, evalGeneralId)
                    newItems[key] = evaluacionesDeEvalGeneral
                }
            }
        }
        evaluacionesPorPolinizadorYEvaluacion = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_operario_evaluacion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val key = evaluacionesPorPolinizadorYEvaluacion.keys.toList()[position]
        val evaluaciones = evaluacionesPorPolinizadorYEvaluacion[key] ?: emptyList()
        val evaluacionGeneralId = key.third

        val totalPalmas = countUniquePalms(evaluaciones, evaluacionGeneralId)
        val totalEventos = evaluaciones
            .filter { it.evaluacionGeneralId == evaluacionGeneralId }
            .sumOf { it.inflorescencia ?: 0 }

        val nombrePolinizador = key.second
        val tituloConEvaluacionId = "$nombrePolinizador"
        holder.bind(tituloConEvaluacionId, totalPalmas, totalEventos)

        // Carga la foto específica usando semana, polinizadorId y evaluacionGeneralId
        CoroutineScope(Dispatchers.Main).launch {
            val photoPath = getPhotoUrl(semana, key.first, evaluacionGeneralId)
            holder.bindPhoto(photoPath)
        }

        holder.itemView.setOnClickListener {
            val nombre = nombrePolinizador.split(" - ").getOrNull(1) ?: nombrePolinizador
            onItemClick(key.first, nombre, evaluacionGeneralId)
        }

        holder.itemView.setOnLongClickListener {
            if (evaluaciones.isNotEmpty()) {
                EvaluacionTableDialog.showEvaluationTableDialog(
                    context = holder.itemView.context,
                    evaluaciones = evaluaciones,
                    evaluadorMap = getEvaluadorMap(),
                    loteMap = getLoteMap()
                )
            } else {
                Toast.makeText(
                    holder.itemView.context,
                    "No hay eventos asociados a esta evaluación",
                    Toast.LENGTH_SHORT
                ).show()
            }
            true
        }

        // Configurar el botón de exportar a Excel
        holder.btnExportToExcel.setOnClickListener {
            if (evaluaciones.isNotEmpty()) {
                // Exportar a Excel usando ExcelUtils
                ExcelUtils.run {
                    fragmentReference.exportToExcel(
                        evaluaciones = evaluaciones,
                        evaluadorMap = getEvaluadorMap(),
                        operarioMap = mapOf(key.first to key.second), // Mapeamos el ID del polinizador a su nombre
                        loteMap = getLoteMap()
                    )
                }
                
                Toast.makeText(
                    holder.itemView.context,
                    "Exportando ${evaluaciones.size} evaluaciones a Excel...",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    holder.itemView.context,
                    "No hay datos para exportar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun getItemCount(): Int = evaluacionesPorPolinizadorYEvaluacion.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombrePolinizador: TextView = itemView.findViewById(R.id.tvPolinizadorNombre)
        private val tvTotalPalmas: TextView = itemView.findViewById(R.id.tvPalmas)
        private val tvTotalEventos: TextView = itemView.findViewById(R.id.tvEventos)
        private val ivEvaluacionFoto: ImageView = itemView.findViewById(R.id.ivEvaluacionFoto)
        val btnExportToExcel: MaterialButton = itemView.findViewById(R.id.btnExportToExcel)

        @SuppressLint("SetTextI18n")
        fun bind(nombreCompleto: String, totalPalmas: Int, totalEventos: Int) {
            tvNombrePolinizador.text = nombreCompleto
            tvTotalPalmas.text = "Palmas: $totalPalmas"
            tvTotalEventos.text = "Eventos: $totalEventos"
        }

        fun bindPhoto(photoPath: String?) {
            ivEvaluacionFoto.load(photoPath ?: R.drawable.baseline_error_24) {
                placeholder(R.drawable.ic_more)
                error(R.drawable.baseline_error_24)
                crossfade(true)
            }
        }
    }
}