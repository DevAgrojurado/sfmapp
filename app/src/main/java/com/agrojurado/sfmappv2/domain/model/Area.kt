package com.agrojurado.sfmappv2.domain.model

data class Area(
    val id: Int = 0,
    var descripcion: String = "",
    var isSynced: Boolean = false // Marcar si está sincronizado
)