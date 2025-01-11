package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.dialogdetail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R

class DetalleEvaluacionAdapter(
    private val items: List<Pair<String, String>>
) : RecyclerView.Adapter<DetalleEvaluacionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val labelTextView: TextView = view.findViewById(R.id.tvLabel)
        val valueTextView: TextView = view.findViewById(R.id.tvValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dialog_evaluacion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.labelTextView.text = item.first
        holder.valueTextView.text = item.second
    }

    override fun getItemCount() = items.size
}