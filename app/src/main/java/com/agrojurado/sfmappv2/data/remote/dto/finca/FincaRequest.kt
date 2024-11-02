package com.agrojurado.sfmappv2.data.remote.dto.finca

import com.google.gson.annotations.SerializedName

data class FincaRequest(

    @SerializedName("id") val id: Int = 0,
    @SerializedName("descripcion") val descripcion: String

)
