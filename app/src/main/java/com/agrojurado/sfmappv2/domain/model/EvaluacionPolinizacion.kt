package com.agrojurado.sfmappv2.domain.model

data class EvaluacionPolinizacion(
    var id: Int? = 0,
    val evaluacionGeneralId: Int? = null,
    var serverId: Int? = null,
    val fecha: String?,
    val hora: String?,
    val semana: Int,
    val ubicacion: String?,
    val idEvaluador: Int,
    val idPolinizador: Int,
    val idlote: Int,
    val seccion: Int,
    val palma: Int?,
    val inflorescencia: Int?,
    val antesis: Int?,
    val antesisDejadas: Int?,
    val postAntesis: Int?,
    val postAntesisDejadas: Int?,
    val espate: Int?,
    val aplicacion: Int?,
    val marcacion: Int?,
    val repaso1: Int?,
    val repaso2: Int?,
    val observaciones: String?,
    var syncStatus: String = "PENDING",
    val timestamp: Long = System.currentTimeMillis()
)