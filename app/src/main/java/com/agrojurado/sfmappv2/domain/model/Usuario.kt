package com.agrojurado.sfmappv2.domain.model

data class Usuario(
    var id: Int=0,
    var codigo: String="",
    var nombre: String="",
    var cedula: String="",
    var email: String="",
    var clave: String="",
    var vigente: Int=0,
    var idcargo: Int=0
)
