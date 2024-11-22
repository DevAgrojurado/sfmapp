package com.agrojurado.sfmappv2.domain.model

data class Finca(
    val id: Int = 0,
    var descripcion: String,
    var isSynced: Boolean = false
)