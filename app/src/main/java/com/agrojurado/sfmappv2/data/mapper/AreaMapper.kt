package com.agrojurado.sfmappv2.data.mapper

import com.agrojurado.sfmappv2.data.local.entity.AreaEntity
import com.agrojurado.sfmappv2.data.remote.dto.area.AreaRequest
import com.agrojurado.sfmappv2.data.remote.dto.area.AreaResponse
import com.agrojurado.sfmappv2.domain.model.Area

object AreaMapper {
    fun toDomain(entity: AreaEntity) = Area(
        id = entity.id,
        descripcion = entity.descripcion
    )

    fun toDatabase(domain: Area) = AreaEntity(
        id = domain.id,
        descripcion = domain.descripcion
    )

    fun toRequest(domain: Area) = AreaRequest(
        id = domain.id,
        descripcion = domain.descripcion
    )

    fun fromResponse(response: AreaResponse) = Area(
        id = response.id,
        descripcion = response.descripcion
    )
}