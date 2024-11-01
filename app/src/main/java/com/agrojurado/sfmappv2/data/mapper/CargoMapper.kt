package com.agrojurado.sfmappv2.data.mapper

import com.agrojurado.sfmappv2.data.local.entity.CargoEntity
import com.agrojurado.sfmappv2.data.remote.dto.cargo.CargoRequest
import com.agrojurado.sfmappv2.data.remote.dto.cargo.CargoResponse
import com.agrojurado.sfmappv2.domain.model.Cargo

object CargoMapper {
    fun toDomain(entity: CargoEntity): Cargo {
        return Cargo(
            id = entity.id,
            descripcion = entity.descripcion
        )
    }

    fun toDatabase(domain: Cargo): CargoEntity {
        return CargoEntity(
            id = domain.id,
            descripcion = domain.descripcion
        )
    }

    fun toRequest(domain: Cargo): CargoRequest {
        return CargoRequest(
            id = domain.id,
            descripcion = domain.descripcion
        )
    }

    fun fromResponse(response: CargoResponse): Cargo {
        return Cargo(
            id = response.id,
            descripcion = response.descripcion
        )
    }
}
