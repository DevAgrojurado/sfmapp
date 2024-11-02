package com.agrojurado.sfmappv2.data.remote.dto.finca

import com.google.gson.annotations.SerializedName

data class FincaResponse(

    @SerializedName("id") val id: Int,
    @SerializedName("descripcion") val descripcion: String

)
