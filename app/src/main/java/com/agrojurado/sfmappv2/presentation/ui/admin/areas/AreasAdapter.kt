package com.agrojurado.sfmappv2.presentation.ui.admin.areas

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
import com.agrojurado.sfmappv2.domain.model.Area
import com.agrojurado.sfmappv2.domain.model.Cargo

class AreasAdapter(
    private val context: Context,
    private var areasList: List<Area>,
    private val onAreaAction: (Area, String) -> Unit
) : RecyclerView.Adapter<AreasAdapter.AreaViewHolder>() {

    inner class AreaViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
        var desc: TextView = v.findViewById(R.id.aTitle)
        var aMenus: ImageView = v.findViewById(R.id.aMenus)

        init {
            aMenus.setOnClickListener { popupMenus(it) }
        }

        private fun popupMenus(v: View) {
            val position = adapterPosition
            val area = areasList[position]
            val popupMenus = PopupMenu(context, v)
            popupMenus.inflate(R.menu.show_menu)
            popupMenus.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.editText -> {
                        val v = LayoutInflater.from(context).inflate(R.layout.add_area, null)
                        val etArea = v.findViewById<EditText>(R.id.et_area)
                        etArea.setText(area.descripcion)

                        AlertDialog.Builder(context)
                            .setView(v)
                            .setPositiveButton("OK") { dialog, _ ->
                                area.descripcion = etArea.text.toString()
                                onAreaAction(area, "update")
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
                                onAreaAction(area, "delete")
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AreaViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.list_areas, parent, false)
        return AreaViewHolder(v)
    }

    override fun onBindViewHolder(holder: AreaViewHolder, position: Int) {
        val area = areasList[position]
        holder.desc.text = area.descripcion
    }

    override fun getItemCount(): Int = areasList.size

    fun updateAreas(newAreas: List<Area>) {
        areasList = newAreas
        notifyDataSetChanged()
    }
}