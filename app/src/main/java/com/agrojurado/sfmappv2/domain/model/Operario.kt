package com.agrojurado.sfmappv2.domain.model

data class Operario(
    var id: Int = 0,
    var codigo: String,
    var nombre: String,
    var cargoId: Int,
    var areaId: Int,
    var fincaId: Int,
    var activo: Boolean = true,
    val isSynced: Boolean = false
)