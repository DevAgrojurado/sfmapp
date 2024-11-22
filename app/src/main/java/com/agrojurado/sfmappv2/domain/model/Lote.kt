package com.agrojurado.sfmappv2.domain.model

data class Lote(
    val id: Int = 0,
    var idFinca : Int?,
    var descripcion: String?,
    var isSynced: Boolean = false

)