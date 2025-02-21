package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.listaevaluacion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.google.android.material.button.MaterialButton

class ListaEvaluacionAdapter(
    private var semanas: List<Int>,
    private val onItemClick: (Int) -> Unit,
    private val onExportPdfClick: (Int) -> Unit,
    private val onExportExcelClick: (Int) -> Unit
) : ListAdapter<Int, ListaEvaluacionAdapter.SemanaViewHolder>(SemanaDiffCallback()) {

    fun updateItems(newItems: List<Int>) {
        semanas = newItems
        notifyDataSetChanged()
    }

    class SemanaDiffCallback : DiffUtil.ItemCallback<Int>() {
        override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SemanaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_semana, parent, false)
        return SemanaViewHolder(view)
    }

    override fun onBindViewHolder(holder: SemanaViewHolder, position: Int) {
        val semana = semanas[position]
        holder.bind(semana)
        holder.itemView.setOnClickListener { onItemClick(semana) }
        //holder.btnExportPdf.setOnClickListener { onExportPdfClick(semana) }
        holder.btnExportExcel.setOnClickListener { onExportExcelClick(semana) } // Nuevo manejador
    }

    override fun getItemCount(): Int = semanas.size

    class SemanaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSemana: TextView = itemView.findViewById(R.id.tvSemana)
        //val btnExportPdf: MaterialButton = itemView.findViewById(R.id.btnExportPdf)
        val btnExportExcel: MaterialButton = itemView.findViewById(R.id.btnExportExcel) // Nuevo botón

        fun bind(semana: Int) {
            tvSemana.text = "Semana $semana"
        }
    }
}