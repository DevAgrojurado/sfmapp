package com.agrojurado.sfmappv2.presentation.ui.admin.usuarios

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
import com.agrojurado.sfmappv2.domain.model.Usuario

class UsuariosAdapter(
    private val context: Context,
    private var usuarios: List<Usuario>,
    private val onUsuarioAction: (Usuario, String) -> Unit
) : RecyclerView.Adapter<UsuariosAdapter.UsuarioViewHolder>() {

    inner class UsuarioViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
        var tvNombre: TextView = v.findViewById(R.id.tvnombreU)
        var tvCodigo: TextView = v.findViewById(R.id.tvCodigoU)
        var tvCargo: TextView = v.findViewById(R.id.tvCargoU)
        var mMenus: ImageView = v.findViewById(R.id.uMenus)

        init {
            mMenus.setOnClickListener { popupMenus(it) }
        }

        private fun popupMenus(v: View) {
            val position = adapterPosition
            val usuario = usuarios[position]
            val popupMenus = PopupMenu(context, v)
            popupMenus.inflate(R.menu.show_menu)
            popupMenus.setOnMenuItemClickListener {
                when (it.itemId) {

                    R.id.delete -> {
                        AlertDialog.Builder(context)
                            .setTitle("Eliminar")
                            .setIcon(R.drawable.ic_warning)
                            .setMessage("¿Estás seguro que deseas eliminar este usuario?")
                            .setPositiveButton("Sí") { dialog, _ ->
                                onUsuarioAction(usuario, "delete")
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.list_usuarios, parent, false)
        return UsuarioViewHolder(v)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = usuarios[position]
        holder.tvNombre.text = usuario.nombre
        holder.tvCodigo.text = usuario.codigo
        holder.tvCargo.text = usuario.email
    }

    override fun getItemCount(): Int = usuarios.size

    fun updateUsuarios(newUsuarios: List<Usuario>) {
        usuarios = newUsuarios
        notifyDataSetChanged()
    }
}