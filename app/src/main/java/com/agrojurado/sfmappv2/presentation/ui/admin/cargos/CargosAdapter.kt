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
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.Cargo


class CargosAdapter(val c: Context,val cargosList:ArrayList<Cargo>):RecyclerView.Adapter<CargosAdapter.CargoViewHolder>()
{


    inner class CargoViewHolder(val v: View):RecyclerView.ViewHolder(v){
        var desc:TextView
        var mMenus:ImageView
        init {
            desc = v.findViewById<TextView>(R.id.mTitle)
            mMenus = v.findViewById(R.id.mMenus)
            mMenus.setOnClickListener{ popupMenus(it) }
        }

        private fun popupMenus(v: View) {
            val position = cargosList[adapterPosition]
            val popupMenus = PopupMenu(c, v)
            popupMenus.inflate(R.menu.show_menu)
            popupMenus.setOnMenuItemClickListener {
                when(it.itemId){
                    R.id.editText->{
                        val v = LayoutInflater.from(c).inflate(R.layout.add_item, null)
                        val cargo = v.findViewById<EditText>(R.id.et_cargo)

                            AlertDialog.Builder(c)
                                .setView(v)
                                .setPositiveButton("OK"){
                                    dialog,_->
                                    position.descripcion = cargo.text.toString()
                                        notifyDataSetChanged()
                                    Toast.makeText(c,"Actualizado exitosamente",Toast.LENGTH_SHORT).show()
                                    dialog.dismiss()
                                }
                                .setNegativeButton("Cancelar"){
                                    dialog,_->
                                    dialog.dismiss()

                                }
                                .create()
                                .show()

                        true
                    }
                    R.id.delete->{
                        AlertDialog.Builder(c)
                            .setTitle("Eliminar")
                            .setIcon(R.drawable.ic_warning)
                            .setMessage("¿Estas seguro que deseas realizar esta acción?")
                            .setPositiveButton("Si"){
                                dialog,_->
                                cargosList.removeAt(adapterPosition)
                                notifyDataSetChanged()
                                Toast.makeText(c, "Eliminado exitosamente", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            }
                            .setNegativeButton("No"){
                                dialog,_->
                                dialog.dismiss()
                            }
                            .create()
                            .show()
                        true
                    }
                    else-> true
                }
            }
            popupMenus.show()
            val popup = PopupMenu::class.java.getDeclaredField("mPopup")
            popup.isAccessible = true
            val menu = popup.get(popupMenus)
            menu.javaClass.getDeclaredMethod("setForceShowIcon",Boolean::class.java)
                .invoke(menu,true)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CargoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v  = inflater.inflate(R.layout.list_cargos,parent,false)
        return CargoViewHolder(v)
    }

    override fun onBindViewHolder(holder: CargoViewHolder, position: Int) {
        val newList = cargosList[position]
        holder.desc.text = newList.descripcion
    }

    override fun getItemCount(): Int {
        return cargosList.size
    }

}