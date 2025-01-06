package com.agrojurado.sfmappv2.domain.security

import android.content.Context
import android.content.SharedPreferences
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.domain.model.UserRoles
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    fun saveCurrentUser(usuario: Usuario) {
        sharedPreferences.edit().apply {
            putInt("user_id", usuario.id)
            putString("user_rol", usuario.rol)
            usuario.idFinca?.let { putInt("user_finca_id", it) } ?: remove("user_finca_id")
            apply()
        }
    }

    fun getCurrentUser(): Usuario? {
        val userId = sharedPreferences.getInt("user_id", -1)
        if (userId == -1) return null

        return Usuario(
            id = userId,
            codigo = "", // Estos campos no son necesarios para la validación
            nombre = "",
            cedula = "",
            email = "",
            clave = "",
            idCargo = null,
            idArea = null,
            idFinca = sharedPreferences.getInt("user_finca_id", -1).takeIf { it != -1 },
            rol = sharedPreferences.getString("user_rol", "") ?: "",
            vigente = 1
        )
    }

    fun clearCurrentUser() {
        sharedPreferences.edit().clear().apply()
    }

    fun isEvaluador(): Boolean {
        return getCurrentUser()?.rol == UserRoles.EVALUADOR.name
    }

    fun getUserFincaId(): Int? {
        val fincaId = sharedPreferences.getInt("user_finca_id", -1)
        return fincaId.takeIf { it != -1 }
    }
}