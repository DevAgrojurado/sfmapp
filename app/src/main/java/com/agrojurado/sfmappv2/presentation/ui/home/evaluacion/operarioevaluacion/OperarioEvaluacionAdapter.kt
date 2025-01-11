package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.operarioevaluacion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion

class OperarioEvaluacionAdapter(
    private var items: List<ItemOperarioEvaluacion>,
    private val onItemClick: (ItemOperarioEvaluacion) -> Unit
) : RecyclerView.Adapter<OperarioEvaluacionAdapter.ViewHolder>() {

    fun updateItems(newItems: List<ItemOperarioEvaluacion>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_operario_evaluacion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNombrePolinizador: TextView = view.findViewById(R.id.tvPolinizadorNombre)
        private val tvCantidadEvaluaciones: TextView = view.findViewById(R.id.tvCantidadEvaluaciones)

        fun bind(item: ItemOperarioEvaluacion) {
            tvNombrePolinizador.text = item.nombrePolinizador
            tvCantidadEvaluaciones.text = "Evaluaciones: ${item.evaluaciones.size}"
        }
    }
}