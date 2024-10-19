package com.agrojurado.sfmappv2.presentation.ui.admin.fincas

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.databinding.ListFincasBinding
import com.agrojurado.sfmappv2.databinding.AddFincaBinding
import com.agrojurado.sfmappv2.domain.model.Finca

class FincasAdapter(
    private val context: Context,
    private var fincasList: List<Finca>,
    private val onFincaAction: (Finca, String) -> Unit
) : RecyclerView.Adapter<FincasAdapter.FincaViewHolder>() {

    inner class FincaViewHolder(val binding: ListFincasBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.mMenus.setOnClickListener { popupMenus(it) }
        }

        private fun popupMenus(v: android.view.View) {
            val position = adapterPosition
            val finca = fincasList[position]
            val popupMenus = PopupMenu(context, v)
            popupMenus.inflate(R.menu.show_menu)
            popupMenus.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.editText -> {
                        val dialogBinding = AddFincaBinding.inflate(LayoutInflater.from(context))
                        dialogBinding.etFinca.setText(finca.descripcion)

                        AlertDialog.Builder(context)
                            .setView(dialogBinding.root)
                            .setPositiveButton("OK") { dialog, _ ->
                                finca.descripcion = dialogBinding.etFinca.text.toString()
                                onFincaAction(finca, "update")
                                notifyItemChanged(position)
                                dialog.dismiss()
                            }
                            .setNegativeButton("Cancelar") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .create()
                            .show()
                        true
                    }
                    R.id.delete -> {
                        AlertDialog.Builder(context)
                            .setTitle("Eliminar")
                            .setIcon(R.drawable.ic_warning)
                            .setMessage("¿Estás seguro que deseas realizar esta acción?")
                            .setPositiveButton("Sí") { dialog, _ ->
                                onFincaAction(finca, "delete")
                                dialog.dismiss()
                            }
                            .setNegativeButton("No") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .create()
                            .show()
                        true
                    }
                    else -> true
                }
            }
            popupMenus.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FincaViewHolder {
        val binding = ListFincasBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FincaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FincaViewHolder, position: Int) {
        val finca = fincasList[position]
        holder.binding.mTitle.text = finca.descripcion
    }

    override fun getItemCount(): Int = fincasList.size

    fun updateFincas(newFincas: List<Finca>) {
        fincasList = newFincas
        notifyDataSetChanged()
    }
}