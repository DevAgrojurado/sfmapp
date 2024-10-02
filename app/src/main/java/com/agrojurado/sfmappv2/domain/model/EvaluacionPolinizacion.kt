package com.agrojurado.sfmappv2.domain.model

data class EvaluacionPolinizacion(
    val id: Long = 0,
    val fecha: Long,
    val hora: String,
    val semana: Int,
    val idEvaluador: String,
    val codigoEvaluador: String,
    val idPolinizador: String,
    val lote: Int,
    val inflorescencia: String,
    val antesis: Int,
    val postAntesis: Int,
    val espate: Int,
    val aplicacion: Int,
    val marcacion: Int,
    val repaso1: Int,
    val repaso2: Int,
    val observaciones: String
)