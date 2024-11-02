package com.agrojurado.sfmappv2.data.mapper

import com.agrojurado.sfmappv2.data.local.entity.FincaEntity
import com.agrojurado.sfmappv2.data.remote.dto.finca.FincaRequest
import com.agrojurado.sfmappv2.data.remote.dto.finca.FincaResponse
import com.agrojurado.sfmappv2.domain.model.Finca

object FincaMapper {
    fun toDomain(entity: FincaEntity): Finca {
        return Finca(
            id = entity.id,
            descripcion = entity.descripcion
        )
    }

    fun toDatabase(domain: Finca): FincaEntity {
        return FincaEntity(
            id = domain.id,
            descripcion = domain.descripcion
        )
    }

    fun fromResponse(response: FincaResponse): Finca {
        return Finca(
            id = response.id,
            descripcion = response.descripcion
        )
    }

    fun toRequest(domain: Finca): FincaRequest {
        return FincaRequest(
            id = domain.id,
            descripcion = domain.descripcion
        )
    }
}
