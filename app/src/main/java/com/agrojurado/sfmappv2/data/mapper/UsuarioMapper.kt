package com.agrojurado.sfmappv2.data.mapper

import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.data.entity.UsuarioEntity

object UsuarioMapper {

    fun toDomain(entity: UsuarioEntity): Usuario {
        return Usuario(
            id = entity.id,
            codigo = entity.codigo,
            nombre = entity.nombre,
            cedula = entity.cedula,
            email = entity.email,
            clave = entity.clave,
            vigente = entity.vigente,
            idCargo = entity.idCargo
        )
    }

    fun toDatabase(model: Usuario): UsuarioEntity {
        return UsuarioEntity(
            id = model.id,
            codigo = model.codigo,
            nombre = model.nombre,
            cedula = model.cedula,
            email = model.email,
            clave = model.clave,
            vigente = model.vigente,
            idCargo = model.idCargo
        )
    }

}