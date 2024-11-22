package com.agrojurado.sfmappv2.data.remote.dto.evaluacion

import com.google.gson.annotations.SerializedName

data class EvaluacionResponse(
    @SerializedName("id") val id: Int=0,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("hora") val hora: String,
    @SerializedName("semana") val semana: Int,
    @SerializedName("ubicacion") val ubicacion: String,
    @SerializedName("idevaluador") val idevaluador: Int,
    @SerializedName("idpolinizador") val idpolinizador: Int,
    @SerializedName("idlote") val idlote: Int,
    @SerializedName("seccion") val seccion: Int,
    @SerializedName("palma") val palma: Int?,
    @SerializedName("inflorescencia") val inflorescencia: Int?,
    @SerializedName("antesis") val antesis: Int?,
    @SerializedName("antesisDejadas") val antesisDejadas: Int?,
    @SerializedName("postantesisDejadas") val postantesisDejadas: Int?,
    @SerializedName("postantesis") val postantesis: Int?,
    @SerializedName("espate") val espate: Int?,
    @SerializedName("aplicacion") val aplicacion: Int?,
    @SerializedName("marcacion") val marcacion: Int?,
    @SerializedName("repaso1") val repaso1: Int?,
    @SerializedName("repaso2") val repaso2: Int?,
    @SerializedName("observaciones") val observaciones: String
)