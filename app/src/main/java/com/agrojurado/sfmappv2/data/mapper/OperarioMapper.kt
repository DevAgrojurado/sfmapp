package com.agrojurado.sfmappv2.data.mapper

import com.agrojurado.sfmappv2.data.entity.OperarioEntity
import com.agrojurado.sfmappv2.domain.model.Operario

object OperarioMapper {

    fun toDomain(entity: OperarioEntity): Operario {
        return Operario(
            id = entity.id,
            codigo = entity.codigo,
            nombre = entity.nombre,
            vigente = entity.vigente,
            //idcargo = entity.idcargo
        )
    }

    fun toDatabase(model: Operario): OperarioEntity {
        return OperarioEntity(
            id = model.id,
            codigo = model.codigo,
            nombre = model.nombre,
            vigente = model.vigente
            //idcargo = model.idcargo
        )
    }
}