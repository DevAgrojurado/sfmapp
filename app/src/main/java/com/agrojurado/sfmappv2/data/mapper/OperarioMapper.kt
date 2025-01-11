package com.agrojurado.sfmappv2.data.mapper

import com.agrojurado.sfmappv2.data.local.entity.OperarioEntity
import com.agrojurado.sfmappv2.data.remote.dto.operario.OperarioRequest
import com.agrojurado.sfmappv2.data.remote.dto.operario.OperarioResponse
import com.agrojurado.sfmappv2.domain.model.Operario

object OperarioMapper {
    fun toDomain(entity: OperarioEntity): Operario {
        return Operario(
            id = entity.id,
            codigo = entity.codigo,
            nombre = entity.nombre,
            cargoId = entity.cargoId,
            areaId = entity.areaId,
            fincaId = entity.fincaId,
            activo = entity.activo
        )
    }

    fun toDatabase(domain: Operario): OperarioEntity {
        return OperarioEntity(
            id = domain.id,
            codigo = domain.codigo,
            nombre = domain.nombre,
            cargoId = domain.cargoId,
            areaId = domain.areaId,
            fincaId = domain.fincaId,
            activo = domain.activo
        )
    }

    fun toRequest(domain: Operario): OperarioRequest {
        return OperarioRequest(
            id = domain.id,
            codigo = domain.codigo,
            nombre = domain.nombre,
            cargoId = domain.cargoId,
            areaId = domain.areaId,
            fincaId = domain.fincaId,
            activo = domain.activo
        )
    }

    fun fromResponse(response: OperarioResponse): Operario {
        return Operario(
            id = response.id,
            codigo = response.codigo,
            nombre = response.nombre,
            cargoId = response.cargoId,
            areaId = response.areaId,
            fincaId = response.fincaId,
            activo = response.activo
        )
    }
}
