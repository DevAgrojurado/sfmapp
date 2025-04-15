package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluaciondetalle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.EvaluacionGeneral

class EvaluacionGeneralAdapter(
    private val onItemClick: (EvaluacionGeneral) -> Unit
) : ListAdapter<EvaluacionGeneral, EvaluacionGeneralAdapter.ViewHolder>(EvaluacionGeneralDiffCallback()) {

    private var operarioMap: Map<Int, String> = emptyMap()
    private var loteMap: Map<Int, String> = emptyMap()

    fun setOperarioMap(map: Map<Int, String>) {
        operarioMap = map
        notifyDataSetChanged()
    }

    fun setLoteMap(map: Map<Int, String>) {
        loteMap = map
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_evaluacion_general, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val evaluacionGeneral = getItem(position)
        val nombrePolinizador = operarioMap[evaluacionGeneral.idpolinizadorev ?: 0] ?: "Desconocido"
        val descripcionLote = loteMap[evaluacionGeneral.idLoteev ?: 0] ?: "Lote ${evaluacionGeneral.idLoteev}"

        holder.bind(evaluacionGeneral, nombrePolinizador, descripcionLote)
        holder.itemView.setOnClickListener { onItemClick(evaluacionGeneral) }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        private val tvPolinizador: TextView = itemView.findViewById(R.id.tvPolinizador)
        private val tvLote: TextView = itemView.findViewById(R.id.tvLote)

        fun bind(evaluacionGeneral: EvaluacionGeneral, nombrePolinizador: String, descripcionLote: String) {
            tvFecha.text = evaluacionGeneral.fecha
            tvPolinizador.text = "Polinizador: $nombrePolinizador"
            tvLote.text = "Lote: $descripcionLote"
        }
    }

    class EvaluacionGeneralDiffCallback : DiffUtil.ItemCallback<EvaluacionGeneral>() {
        override fun areItemsTheSame(oldItem: EvaluacionGeneral, newItem: EvaluacionGeneral) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: EvaluacionGeneral, newItem: EvaluacionGeneral) =
            oldItem == newItem
    }
}