package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.operarioevaluacion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion

class OperarioEvaluacionAdapter(
    private var evaluacionesPorPolinizador: Map<Pair<Int, String>, List<EvaluacionPolinizacion>> = emptyMap(),
    private val onItemClick: (Int, String) -> Unit
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
        holder.bind(polinizador.second, evaluaciones.size)
        holder.itemView.setOnClickListener {
            onItemClick(polinizador.first, polinizador.second)
        }
    }

    override fun getItemCount() = evaluacionesPorPolinizador.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNombrePolinizador: TextView = view.findViewById(R.id.tvPolinizadorNombre)
        private val tvCantidadEvaluaciones: TextView = view.findViewById(R.id.tvCantidadEvaluaciones)

        fun bind(nombrePolinizador: String, cantidadEvaluaciones: Int) {
            tvNombrePolinizador.text = nombrePolinizador
            tvCantidadEvaluaciones.text = "Evaluaciones: $cantidadEvaluaciones"
        }
    }
}