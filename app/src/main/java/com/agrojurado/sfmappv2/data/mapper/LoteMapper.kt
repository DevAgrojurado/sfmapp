package com.agrojurado.sfmappv2.data.mapper

import com.agrojurado.sfmappv2.data.local.entity.LoteEntity
import com.agrojurado.sfmappv2.domain.model.Lote

object LoteMapper {
    fun toDomain(entity: LoteEntity): Lote {
        return Lote(
            id = entity.id,
            descripcion = entity.descripcion,
            idFinca = entity.idFinca
        )
    }

    //**** this fun searches for the entity ****//

    fun toDatabase(domain: Lote): LoteEntity {
        return LoteEntity(
            id = domain.id,
            descripcion = domain.descripcion,
            idFinca = domain.idFinca
        )
    }
}
