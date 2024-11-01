package com.agrojurado.sfmappv2.data.mapper

import com.agrojurado.sfmappv2.data.local.entity.FincaEntity
import com.agrojurado.sfmappv2.domain.model.Finca

object FincaMapper {
    fun toDomain(entity: FincaEntity): Finca {
        return Finca(
            id = entity.id,
            descripcion = entity.descripcion
        )
    }

    //**** this fun searches for the entity ****//

    fun toDatabase(domain: Finca): FincaEntity {
        return FincaEntity(
            id = domain.id,
            descripcion = domain.descripcion
        )
    }
}
