package com.agrojurado.sfmappv2.presentation.ui.admin.lotes

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
import com.agrojurado.sfmappv2.domain.model.Finca
import com.agrojurado.sfmappv2.domain.model.Lote
class LotesAdapter(
    private val context: Context,
    private var lotesList: List<Lote>,
    private var fincasList: List<Finca>,
    private val onLoteAction: (Lote, String) -> Unit
) : RecyclerView.Adapter<LotesAdapter.LoteViewHolder>() {

    inner class LoteViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
        var descripcion: TextView = v.findViewById(R.id.tvLoteDesc)
        var finca: TextView = v.findViewById(R.id.tvFincaL)
        var mMenus: ImageView = v.findViewById(R.id.lMenus)

        init {
            mMenus.setOnClickListener { popupMenus(it) }
        }

        private fun popupMenus(v: View) {
            val position = adapterPosition
            val lote = lotesList[position]
            val popupMenus = PopupMenu(context, v)
            popupMenus.inflate(R.menu.show_menu)
            popupMenus.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.editText -> {
                        val dialogView = LayoutInflater.from(context).inflate(R.layout.add_lote, null)
                        val etLote = dialogView.findViewById<EditText>(R.id.et_lotes)
                        val spinnerFinca = dialogView.findViewById<Spinner>(R.id.spinnerFincaL)

                        etLote.setText(lote.descripcion)
                        setupSpinner(spinnerFinca, fincasList.map { it.descripcion })

                        setSpinnerSelection(spinnerFinca, fincasList, lote.idFinca)

                        AlertDialog.Builder(context)
                            .setView(dialogView)
                            .setPositiveButton("OK") { dialog, _ ->
                                lote.descripcion = etLote.text.toString()
                                lote.idFinca = fincasList[spinnerFinca.selectedItemPosition].id
                                onLoteAction(lote, "update")
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
                            .setMessage("¿Estás seguro que deseas eliminar este lote?")
                            .setPositiveButton("Sí") { dialog, _ ->
                                onLoteAction(lote, "delete")
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoteViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.list_lotes, parent, false)
        return LoteViewHolder(v)
    }

    override fun onBindViewHolder(holder: LoteViewHolder, position: Int) {
        val lote = lotesList[position]
        holder.descripcion.text = lote.descripcion
        holder.finca.text = fincasList.find { it.id == lote.idFinca }?.descripcion ?: "Sin finca"
    }

    override fun getItemCount(): Int = lotesList.size

    fun updateLotes(newLotes: List<Lote>) {
        lotesList = newLotes
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
        val position = list.indexOfFirst { (it as? Finca)?.id == id }
        if (position != -1) {
            spinner.setSelection(position)
        }
    }
}