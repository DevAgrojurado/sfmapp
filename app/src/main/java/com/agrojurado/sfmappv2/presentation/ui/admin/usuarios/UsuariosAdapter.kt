package com.agrojurado.sfmappv2.presentation.ui.admin.usuarios

import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.databinding.ListUsuariosBinding
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class UsuariosAdapter(
    private val context: Context,
    private var usuarios: List<Usuario>,
    private val onUsuarioAction: (Usuario, String) -> Unit
) : RecyclerView.Adapter<UsuariosAdapter.UsuarioViewHolder>() {

    inner class UsuarioViewHolder(val binding: ListUsuariosBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(usuario: Usuario) {
            binding.apply {
                tvnombreU.text = usuario.nombre
                tvCodigoU.text = usuario.codigo
                tvCargoU.text = usuario.email

                uMenus.setOnClickListener { showPopupMenu(usuario, it) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val binding = ListUsuariosBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UsuarioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        holder.bind(usuarios[position])
    }

    override fun getItemCount(): Int = usuarios.size

    private fun showPopupMenu(usuario: Usuario, view: View) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.show_menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.editText -> {
                    showUpdateDialog(usuario)
                    true
                }
                R.id.delete -> {
                    showDeleteConfirmDialog(usuario)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showUpdateDialog(usuario: Usuario) {
        val dialogBuilder = MaterialAlertDialogBuilder(context)

        // Crear un LinearLayout contenedor
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dpToPx(24), // left
                dpToPx(16), // top
                dpToPx(24), // right
                dpToPx(16)  // bottom
            )
        }

        // Crear TextInputLayouts con validación
        val camposActualizacion = listOf(
            "Nombre" to usuario.nombre,
            "Email" to usuario.email,
            "Código" to usuario.codigo
        )

        val editTexts = camposActualizacion.map { (hint, valorActual) ->
            val textInputLayout = TextInputLayout(context).apply {
                this.hint = hint
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dpToPx(8)
                }
                boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            }

            val editText = TextInputEditText(context).apply {
                setText(valorActual)

                // Validación en tiempo real
                doAfterTextChanged { text ->
                    textInputLayout.error = when {
                        hint == "Nombre" && text.isNullOrBlank() -> "El nombre no puede estar vacío"
                        hint == "Email" && !isValidEmail(text.toString()) -> "Email inválido"
                        hint == "Código" && text.isNullOrBlank() -> "El código no puede estar vacío"
                        else -> null
                    }
                }
            }

            // Configurar tipo de entrada según el campo
            when (hint) {
                "Nombre" -> editText.inputType = InputType.TYPE_TEXT_VARIATION_PERSON_NAME
                "Email" -> editText.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                "Código" -> editText.inputType = InputType.TYPE_CLASS_TEXT
            }

            textInputLayout.addView(editText)
            container.addView(textInputLayout)

            editText
        }

        // Mostrar diálogo
        dialogBuilder
            .setTitle("Actualizar Usuario")
            .setView(container)
            .setPositiveButton("Guardar") { dialog, _ ->
                // Validación final antes de guardar
                val isValid = editTexts.all { it.text?.isNotBlank() == true } &&
                        isValidEmail(editTexts[1].text.toString())

                if (isValid) {
                    val updatedUsuario = usuario.copy(
                        nombre = editTexts[0].text?.toString() ?: usuario.nombre,
                        email = editTexts[1].text?.toString() ?: usuario.email,
                        codigo = editTexts[2].text?.toString() ?: usuario.codigo
                    )
                    onUsuarioAction(updatedUsuario, "edit")
                    dialog.dismiss()
                } else {
                    Toast.makeText(context, "Por favor, complete todos los campos correctamente", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showDeleteConfirmDialog(usuario: Usuario) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Eliminar Usuario")
            .setMessage("¿Estás seguro de que deseas eliminar a ${usuario.nombre}?")
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton("Sí") { dialog, _ ->
                onUsuarioAction(usuario, "delete")
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Función de validación de email
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Función de utilidad para convertir dp a px
    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    fun updateUsuarios(newUsuarios: List<Usuario>) {
        usuarios = newUsuarios
        notifyDataSetChanged()
    }
}