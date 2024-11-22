package com.agrojurado.sfmappv2.data.mapper

import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.data.local.entity.UsuarioEntity
import com.agrojurado.sfmappv2.data.remote.dto.usuario.UsuarioRequest
import com.agrojurado.sfmappv2.data.remote.dto.usuario.UsuarioResponse

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
            idCargo = entity.idCargo,
            idArea = entity.idArea,
            idFinca = entity.idFinca
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
            idCargo = model.idCargo,
            idArea = model.idArea,
            idFinca = model.idFinca
        )
    }

    fun toRequest(domain: Usuario): UsuarioRequest {
        return UsuarioRequest(
            id = domain.id,
            codigo = domain.codigo,
            nombre = domain.nombre,
            cedula = domain.cedula,
            email = domain.email,
            clave = domain.clave,
            vigente = domain.vigente,
            idCargo = domain.idCargo,
            idArea = domain.idArea,
            idFinca = domain.idFinca
        )
    }


    fun fromResponse(response: UsuarioResponse): Usuario {
        return Usuario(
            id = response.id,
            codigo = response.codigo ?: "",
            nombre = response.nombre ?: "",
            cedula = response.cedula ?: "",
            email = response.email ?: "",
            clave = response.clave ?: "",
            vigente = response.vigente ?: 0,
            idCargo = response.idCargo,
            idArea = response.idArea,
            idFinca = response.idFinca
        )
    }
}