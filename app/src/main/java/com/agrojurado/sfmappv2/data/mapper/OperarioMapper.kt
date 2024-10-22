package com.agrojurado.sfmappv2.data.mapper

import com.agrojurado.sfmappv2.data.entity.OperarioEntity
import com.agrojurado.sfmappv2.domain.model.Operario

object OperarioMapper {
    fun toDomain(entity: OperarioEntity): Operario {
        return Operario(
            id = entity.id,
            codigo = entity.codigo,
            nombre = entity.nombre,
            cargoId = entity.cargoId,
            areaId = entity.areaId,
            fincaId = entity.fincaId
        )
    }

    fun toDatabase(domain: Operario): OperarioEntity {
        return OperarioEntity(
            id = domain.id,
            codigo = domain.codigo,
            nombre = domain.nombre,
            cargoId = domain.cargoId,
            areaId = domain.areaId,
            fincaId = domain.fincaId
        )
    }
}
