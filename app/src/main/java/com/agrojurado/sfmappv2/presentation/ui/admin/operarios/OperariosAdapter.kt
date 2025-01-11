package com.agrojurado.sfmappv2.presentation.ui.admin.operarios

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.Area
import com.agrojurado.sfmappv2.domain.model.Cargo
import com.agrojurado.sfmappv2.domain.model.Finca
import com.agrojurado.sfmappv2.domain.model.Operario

class OperariosAdapter(
    private val context: Context,
    private var operariosList: List<Operario>,
    private var cargosList: List<Cargo>,
    private var areasList: List<Area>,
    private var fincasList: List<Finca>,
    private val onOperarioAction: (Operario, String) -> Unit
) : RecyclerView.Adapter<OperariosAdapter.OperarioViewHolder>() {

    inner class OperarioViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
        var codigo: TextView = v.findViewById(R.id.tvCodigo)
        var nombre: TextView = v.findViewById(R.id.tvNombre)
        var cargo: TextView = v.findViewById(R.id.tvCargo)
        var estado: TextView = v.findViewById(R.id.tvEstado)
        var switchActivo: SwitchCompat = v.findViewById(R.id.switchActivo)
        var mMenus: ImageView = v.findViewById(R.id.mMenus)

        init {
            mMenus.setOnClickListener { popupMenus(it) }
        }

        private fun popupMenus(v: View) {
            val position = adapterPosition
            val operario = operariosList[position]
            val popupMenus = PopupMenu(context, v)
            popupMenus.inflate(R.menu.show_menu)
            popupMenus.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.editText -> {
                        onOperarioAction(operario, "update")
                        true
                    }
                    R.id.delete -> {
                        AlertDialog.Builder(context)
                            .setTitle("Eliminar")
                            .setIcon(R.drawable.ic_warning)
                            .setMessage("¿Estás seguro que deseas eliminar este operario?")
                            .setPositiveButton("Sí") { dialog, _ ->
                                onOperarioAction(operario, "delete")
                                dialog.dismiss()
                            }
                            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OperarioViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.list_operarios, parent, false)
        return OperarioViewHolder(v)
    }

    override fun onBindViewHolder(holder: OperarioViewHolder, position: Int) {
        val operario = operariosList[position]

        holder.codigo.text = operario.codigo
        holder.nombre.text = operario.nombre
        holder.cargo.text = cargosList.find { it.id == operario.cargoId }?.descripcion ?: "Sin cargo"

        // Evita disparar el listener mientras configuras el estado inicial
        holder.switchActivo.setOnCheckedChangeListener(null)
        holder.switchActivo.isChecked = operario.activo
        holder.estado.text = if (operario.activo) "Activo" else "Inactivo"

        // Configura el listener para el switch
        holder.switchActivo.setOnCheckedChangeListener { _, isChecked ->
            holder.estado.text = if (isChecked) "Activo" else "Inactivo"
            val updatedOperario = operario.copy(activo = isChecked)
            onOperarioAction(updatedOperario, "updateState")
        }
    }

    override fun getItemCount(): Int = operariosList.size

    fun updateOperarios(newOperarios: List<Operario>) {
        operariosList = newOperarios
        notifyDataSetChanged()
    }

    fun setCargos(cargos: List<Cargo>) {
        this.cargosList = cargos
        notifyDataSetChanged()
    }

    fun setAreas(areas: List<Area>) {
        this.areasList = areas
        notifyDataSetChanged()
    }

    fun setFincas(fincas: List<Finca>) {
        this.fincasList = fincas
        notifyDataSetChanged()
    }

    private fun setupSpinner(spinner: Spinner, items: List<String>) {
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun <T> setSpinnerSelection(spinner: Spinner, list: List<T>, id: Int) {
        val position = list.indexOfFirst { (it as? Cargo)?.id == id || (it as? Area)?.id == id || (it as? Finca)?.id == id }
        if (position != -1) {
            spinner.setSelection(position)
        }
    }
}