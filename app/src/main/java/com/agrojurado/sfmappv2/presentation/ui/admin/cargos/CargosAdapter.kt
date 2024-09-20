package com.agrojurado.sfmappv2.presentation.ui.admin.cargos

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.Cargo

class CargosAdapter(
    private val context: Context,
    private var cargosList: List<Cargo>,
    private val onCargoAction: (Cargo, String) -> Unit
) : RecyclerView.Adapter<CargosAdapter.CargoViewHolder>() {

    inner class CargoViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
        var desc: TextView = v.findViewById(R.id.mTitle)
        var mMenus: ImageView = v.findViewById(R.id.mMenus)

        init {
            mMenus.setOnClickListener { popupMenus(it) }
        }

        private fun popupMenus(v: View) {
            val position = adapterPosition
            val cargo = cargosList[position]
            val popupMenus = PopupMenu(context, v)
            popupMenus.inflate(R.menu.show_menu)
            popupMenus.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.editText -> {
                        val v = LayoutInflater.from(context).inflate(R.layout.add_item, null)
                        val etCargo = v.findViewById<EditText>(R.id.et_cargo)
                        etCargo.setText(cargo.descripcion)

                        AlertDialog.Builder(context)
                            .setView(v)
                            .setPositiveButton("OK") { dialog, _ ->
                                cargo.descripcion = etCargo.text.toString()
                                onCargoAction(cargo, "update")
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
                                onCargoAction(cargo, "delete")
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CargoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.list_cargos, parent, false)
        return CargoViewHolder(v)
    }

    override fun onBindViewHolder(holder: CargoViewHolder, position: Int) {
        val cargo = cargosList[position]
        holder.desc.text = cargo.descripcion
    }

    override fun getItemCount(): Int = cargosList.size

    fun updateCargos(newCargos: List<Cargo>) {
        cargosList = newCargos
        notifyDataSetChanged()
    }
}