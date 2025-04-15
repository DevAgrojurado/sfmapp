package com.agrojurado.sfmappv2.data.remote.dto.evaluaciongeneral

import com.google.gson.annotations.SerializedName

data class EvaluacionGeneralRequest(
    @SerializedName("id") val id: Int?,
    @SerializedName("fecha") val fecha: String?,
    @SerializedName("hora") val hora: String?,
    @SerializedName("semana") val semana: Int?,
    @SerializedName("idevaluadorev") val idevaluadorev: Int,
    @SerializedName("idpolinizadorev") val idpolinizadorev: Int?,
    @SerializedName("idloteev") val idloteev: Int?,
    @SerializedName("fotopath") val fotopath: String?,
    @SerializedName("firmapath") val firmapath: String?,
    @SerializedName("timestamp") val timestamp: Long
)