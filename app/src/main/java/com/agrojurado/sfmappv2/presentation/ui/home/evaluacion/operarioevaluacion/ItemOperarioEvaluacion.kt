package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.operarioevaluacion

import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion

data class ItemOperarioEvaluacion(
    val nombrePolinizador: String,
    val evaluaciones: List<EvaluacionPolinizacion>
)
