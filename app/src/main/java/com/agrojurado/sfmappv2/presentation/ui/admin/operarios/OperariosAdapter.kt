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
                        val dialogView = LayoutInflater.from(context).inflate(R.layout.add_operario, null)
                        val etCodigo = dialogView.findViewById<EditText>(R.id.et_codigo)
                        val etNombre = dialogView.findViewById<EditText>(R.id.et_nombre)
                        val spinnerCargo = dialogView.findViewById<Spinner>(R.id.spinnerCargo)
                        val spinnerArea = dialogView.findViewById<Spinner>(R.id.spinnerArea)
                        val spinnerFinca = dialogView.findViewById<Spinner>(R.id.spinnerFinca)

                        etCodigo.setText(operario.codigo)
                        etNombre.setText(operario.nombre)

                        setupSpinner(spinnerCargo, cargosList.map { it.descripcion })
                        setupSpinner(spinnerArea, areasList.map { it.descripcion })
                        setupSpinner(spinnerFinca, fincasList.map { it.descripcion })

                        setSpinnerSelection(spinnerCargo, cargosList, operario.cargoId)
                        setSpinnerSelection(spinnerArea, areasList, operario.areaId)
                        setSpinnerSelection(spinnerFinca, fincasList, operario.fincaId)

                        AlertDialog.Builder(context)
                            .setView(dialogView)
                            .setPositiveButton("OK") { dialog, _ ->
                                operario.codigo = etCodigo.text.toString()
                                operario.nombre = etNombre.text.toString()
                                operario.cargoId = cargosList[spinnerCargo.selectedItemPosition].id
                                operario.areaId = areasList[spinnerArea.selectedItemPosition].id
                                operario.fincaId = fincasList[spinnerFinca.selectedItemPosition].id
                                onOperarioAction(operario, "update")
                                notifyItemChanged(position)
                                dialog.dismiss()
                            }
                            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
                            .create()
                            .show()
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