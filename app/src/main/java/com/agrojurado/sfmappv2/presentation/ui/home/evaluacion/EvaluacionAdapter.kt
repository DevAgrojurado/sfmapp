package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion

class EvaluacionAdapter(private val onItemClick: (EvaluacionPolinizacion, String) -> Unit) :
    ListAdapter<EvaluacionPolinizacion, EvaluacionAdapter.EvaluacionViewHolder>(EvaluacionDiffCallback()) {

    private var operarioMap: Map<Int, String> = emptyMap()

    fun setOperarioMap(map: Map<Int, String>) {
        operarioMap = map
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EvaluacionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_evaluacion, parent, false)
        return EvaluacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: EvaluacionViewHolder, position: Int) {
        val evaluacion = getItem(position)
        val nombrePolinizador = operarioMap[evaluacion.idPolinizador] ?: "Desconocido"
        holder.bind(evaluacion, nombrePolinizador)
        holder.itemView.setOnClickListener {
            onItemClick(evaluacion, nombrePolinizador)
        }
    }

    class EvaluacionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPolinizador: TextView = itemView.findViewById(R.id.tvPolinizador)
        private val tvLote: TextView = itemView.findViewById(R.id.tvLote)

        fun bind(evaluacion: EvaluacionPolinizacion, nombrePolinizador: String) {
            tvPolinizador.text = "$nombrePolinizador"
            tvLote.text = "Lote: ${evaluacion.lote}"
        }
    }

    class EvaluacionDiffCallback : DiffUtil.ItemCallback<EvaluacionPolinizacion>() {
        override fun areItemsTheSame(oldItem: EvaluacionPolinizacion, newItem: EvaluacionPolinizacion): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: EvaluacionPolinizacion, newItem: EvaluacionPolinizacion): Boolean {
            return oldItem == newItem
        }
    }
}