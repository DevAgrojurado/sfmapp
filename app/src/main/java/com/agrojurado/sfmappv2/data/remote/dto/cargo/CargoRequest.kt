package com.agrojurado.sfmappv2.data.remote.dto.cargo

import com.google.gson.annotations.SerializedName

data class CargoRequest(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("descripcion") val descripcion: String
)