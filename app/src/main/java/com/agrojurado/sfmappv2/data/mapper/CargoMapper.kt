package com.agrojurado.sfmappv2.data.mapper

import com.agrojurado.sfmappv2.data.entity.CargoEntity
import com.agrojurado.sfmappv2.domain.model.Cargo

object CargoMapper {
    fun toDomain(entity: CargoEntity): Cargo {
        return Cargo(
            id = entity.id,
            descripcion = entity.descripcion
        )
    }

    //**** this fun searches for the entity ****//

    fun toDatabase(domain: Cargo): CargoEntity {
        return CargoEntity(
            id = domain.id,
            descripcion = domain.descripcion
        )
    }
}
