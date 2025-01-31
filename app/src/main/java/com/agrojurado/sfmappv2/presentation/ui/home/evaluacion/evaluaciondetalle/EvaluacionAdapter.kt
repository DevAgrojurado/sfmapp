package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluaciondetalle

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion

class EvaluacionAdapter(
    private val onItemClick: (EvaluacionPolinizacion, String) -> Unit,
    private val onEvaluacionAction: (EvaluacionPolinizacion, String) -> Unit
) : ListAdapter<EvaluacionPolinizacion, EvaluacionAdapter.EvaluacionViewHolder>(
    EvaluacionDiffCallback()
) {

    private var operarioMap: Map<Int, String> = emptyMap()
    private var loteMap: Map<Int, String> = emptyMap()  // Nuevo mapa para los lotes

    fun setOperarioMap(map: Map<Int, String>) {
        operarioMap = map
        notifyDataSetChanged()
    }

    fun setLoteMap(map: Map<Int, String>) {  // Nuevo método para actualizar el mapa de lotes
        loteMap = map
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EvaluacionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_evaluacion, parent, false)
        return EvaluacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: EvaluacionViewHolder, position: Int) {
        val evaluacion = getItem(position)
        val nombrePolinizador = operarioMap[evaluacion.idPolinizador] ?: "Desconocido"
        val descripcionLote = loteMap[evaluacion.idlote] ?: "Lote ${evaluacion.idlote}"  // Usar la descripción del lote
        holder.bind(evaluacion, nombrePolinizador, descripcionLote)

        holder.mMenus.setOnClickListener { view ->
            showPopupMenu(view, evaluacion, nombrePolinizador)
        }

        holder.itemView.setOnClickListener {
            onItemClick(evaluacion, nombrePolinizador)
        }
    }

    private fun showPopupMenu(view: View, evaluacion: EvaluacionPolinizacion, nombrePolinizador: String) {
        val popupMenu = PopupMenu(view.context, view)
        popupMenu.inflate(R.menu.show_menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.delete -> {
                    showDeleteConfirmation(view.context, evaluacion)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showDeleteConfirmation(context: android.content.Context, evaluacion: EvaluacionPolinizacion) {
        AlertDialog.Builder(context)
            .setTitle("Eliminar Evaluación")
            .setIcon(R.drawable.ic_warning)
            .setMessage("¿Estás seguro que deseas eliminar esta evaluación?")
            .setPositiveButton("Sí") { dialog, _ ->
                onEvaluacionAction(evaluacion, "delete")
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    class EvaluacionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPolinizador: TextView = itemView.findViewById(R.id.tvPolinizador)
        private val tvLote: TextView = itemView.findViewById(R.id.tvLote)
        private val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        val mMenus: ImageView = itemView.findViewById(R.id.mMenus)

        fun bind(evaluacion: EvaluacionPolinizacion, nombrePolinizador: String, descripcionLote: String) {
            tvPolinizador.text = nombrePolinizador
            tvFecha.text = evaluacion.fecha
            tvLote.text = "Lote:\n$descripcionLote" // Mostrar la descripción del lote directamente
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