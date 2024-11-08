package com.agrojurado.sfmappv2.data.remote.dto.operario

import com.google.gson.annotations.SerializedName

data class OperarioRequest(

    @SerializedName("id") val id: Int = 0,
    @SerializedName("codigo") val codigo: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("cargoId") val cargoId: Int,
    @SerializedName("areaId") val areaId: Int,
    @SerializedName("fincaId") val fincaId: Int

)
