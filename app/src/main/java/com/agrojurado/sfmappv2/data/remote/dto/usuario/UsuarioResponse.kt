package com.agrojurado.sfmappv2.data.remote.dto.usuario

import com.google.gson.annotations.SerializedName

data class UsuarioResponse(

    @SerializedName("id") val id: Int,
    @SerializedName("codigo") val codigo: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("cedula") val cedula: String,
    @SerializedName("email") val email: String,
    @SerializedName("clave") val clave: String,
    @SerializedName("idCargo") val idCargo: Int?,
    @SerializedName("idArea") val idArea: Int?,
    @SerializedName("idFinca") val idFinca: Int?,
    @SerializedName("vigente") val vigente: Int

)
