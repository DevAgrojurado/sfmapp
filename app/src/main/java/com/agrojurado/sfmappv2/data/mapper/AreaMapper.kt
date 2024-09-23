package com.agrojurado.sfmappv2.data.mapper

import com.agrojurado.sfmappv2.data.entity.AreaEntity
import com.agrojurado.sfmappv2.domain.model.Area

object AreaMapper {
    fun toDomain(entity: AreaEntity): Area {
        return Area(
            id = entity.id,
            descripcion = entity.descripcion
        )
    }

    //**** this function searches for the entity ****//

    fun toDatabase(domain: Area): AreaEntity {
        return AreaEntity(
            id = domain.id,
            descripcion = domain.descripcion
        )
    }
}
