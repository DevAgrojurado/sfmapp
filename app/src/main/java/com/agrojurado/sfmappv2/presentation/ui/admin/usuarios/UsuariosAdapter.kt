package com.agrojurado.sfmappv2.presentation.ui.admin.usuarios

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.Usuario

class UsuariosAdapter(private var usuarios: List<Usuario>) : RecyclerView.Adapter<UsuariosAdapter.UsuarioViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_usuarios, parent, false)
        return UsuarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = usuarios[position]
        holder.bind(usuario)
    }

    override fun getItemCount(): Int = usuarios.size

    class UsuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre: TextView = itemView.findViewById(R.id.tvnombreU)
        private val tvCodigo: TextView = itemView.findViewById(R.id.tvCodigoU)
        private val tvCargo: TextView = itemView.findViewById(R.id.tvCargoU)

        fun bind(usuario: Usuario) {
            tvNombre.text = usuario.nombre
            tvCodigo.text = usuario.codigo
            tvCargo.text = usuario.email // Asegúrate de que el campo existe en tu modelo
        }
    }

    fun updateUsuarios(newUsuarios: List<Usuario>) {
        usuarios = newUsuarios
        notifyDataSetChanged() // Notifica al adaptador que los datos han cambiado
    }
}
