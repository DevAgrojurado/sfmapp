package com.agrojurado.sfmappv2.data.remote.dto.area

import com.google.gson.annotations.SerializedName

data class AreaResponse(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("descripcion") val descripcion: String
)