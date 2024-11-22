package com.agrojurado.sfmappv2.data.remote.dto.login

import com.agrojurado.sfmappv2.data.remote.dto.usuario.UsuarioResponse
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("usuario") val usuario: UsuarioResponse?
)