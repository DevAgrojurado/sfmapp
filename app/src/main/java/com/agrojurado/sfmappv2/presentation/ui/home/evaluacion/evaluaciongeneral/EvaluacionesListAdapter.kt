package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluaciongeneral

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion

class EvaluacionesListAdapter(
    private val onItemClickListener: (EvaluacionPolinizacion) -> Unit,
    private val onDeleteClickListener: (EvaluacionPolinizacion) -> Unit
) : ListAdapter<EvaluacionPolinizacion, EvaluacionesListAdapter.EvaluacionViewHolder>(EvaluacionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EvaluacionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_evaluacion, parent, false)
        return EvaluacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: EvaluacionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EvaluacionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLoteSeccion: TextView = itemView.findViewById(R.id.tvLoteSeccion)
        private val tvPalma: TextView = itemView.findViewById(R.id.tvPalma)
        private val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        private val tvEstadoSync: TextView = itemView.findViewById(R.id.tvEstadoSync)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(evaluacion: EvaluacionPolinizacion) {
            // Configurar datos en las vistas
            tvLoteSeccion.text = "Lote: ${evaluacion.idlote} - Sección: ${evaluacion.seccion}"
            tvPalma.text = "Palma: ${evaluacion.palma ?: "N/A"}"
            tvFecha.text = "Fecha: ${evaluacion.fecha} ${evaluacion.hora}"

            // Estado de sincronización
            if (evaluacion.syncStatus == "SYNCED") {
                tvEstadoSync.text = "Sincronizado"
                tvEstadoSync.setTextColor(itemView.context.getColor(R.color.green))
            } else {
                tvEstadoSync.text = "Pendiente"
                tvEstadoSync.setTextColor(itemView.context.getColor(R.color.black))
            }

            // Configurar listeners
            itemView.setOnClickListener {
                onItemClickListener(evaluacion)
            }

            btnDelete.setOnClickListener {
                onDeleteClickListener(evaluacion)
            }
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