package com.agrojurado.sfmappv2.data.remote.dto.lote

import com.google.gson.annotations.SerializedName

data class LoteRequest(

    @SerializedName("id") val id: Int,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("idFinca") val idFinca: Int?

)
