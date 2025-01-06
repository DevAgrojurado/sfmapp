package com.agrojurado.sfmappv2.data.remote.dto.login

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("clave") val clave: String,
)