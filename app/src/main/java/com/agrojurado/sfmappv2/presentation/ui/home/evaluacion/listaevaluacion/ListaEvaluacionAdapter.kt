package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.listaevaluacion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R

class ListaEvaluacionAdapter(
    private var semanas: List<Int>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<ListaEvaluacionAdapter.SemanaViewHolder>() {

    fun updateItems(newItems: List<Int>) {
        semanas = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SemanaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_semana, parent, false)
        return SemanaViewHolder(view)
    }

    override fun onBindViewHolder(holder: SemanaViewHolder, position: Int) {
        val semana = semanas[position]
        holder.bind(semana)
        holder.itemView.setOnClickListener { onItemClick(semana) }
    }

    override fun getItemCount(): Int = semanas.size

    class SemanaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSemana: TextView = itemView.findViewById(R.id.tvSemana)

        fun bind(semana: Int) {
            tvSemana.text = "Semana $semana"
        }
    }
}