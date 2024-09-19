package com.agrojurado.sfmappv2.data.mapper

import com.agrojurado.sfmappv2.data.entity.CargoEntity
import com.agrojurado.sfmappv2.domain.model.Cargo

object CargoMapper {
    fun mapToDomain(entity: CargoEntity): Cargo {
        return Cargo(
            id = entity.id,
            descripcion = entity.descripcion
        )
    }

    fun mapToEntity(domain: Cargo): CargoEntity {
        return CargoEntity(
            id = domain.id,
            descripcion = domain.descripcion
        )
    }
}
