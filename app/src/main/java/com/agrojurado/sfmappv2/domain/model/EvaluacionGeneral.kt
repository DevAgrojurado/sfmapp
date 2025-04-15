package com.agrojurado.sfmappv2.domain.model

data class EvaluacionGeneral(
    val id: Int = 0,
    val serverId: Int? = null,
    val fecha: String,
    val hora: String,
    val semana: Int,
    val idevaluadorev: Int,
    val idpolinizadorev: Int?,
    val idLoteev: Int?,
    val isSynced: Boolean = false,
    val isTemporary: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val fotoPath: String?,
    val firmaPath: String?
)