package com.agrojurado.sfmappv2.data.mapper

import com.agrojurado.sfmappv2.data.local.entity.LoteEntity
import com.agrojurado.sfmappv2.data.remote.dto.lote.LoteRequest
import com.agrojurado.sfmappv2.data.remote.dto.lote.LoteResponse
import com.agrojurado.sfmappv2.domain.model.Lote

object LoteMapper {
    fun toDomain(entity: LoteEntity): Lote {
        return Lote(
            id = entity.id,
            descripcion = entity.descripcion,
            idFinca = entity.idFinca
        )
    }

    fun toDatabase(domain: Lote): LoteEntity {
        return LoteEntity(
            id = domain.id,
            descripcion = domain.descripcion,
            idFinca = domain.idFinca
        )
    }

    fun fromResponse(response: LoteResponse): Lote {
        return Lote(
            id = response.id,
            descripcion = response.descripcion,
            idFinca = response.idFinca
        )
    }

    fun toRequest(domain: Lote): LoteRequest {
        return LoteRequest(
            id = domain.id,
            descripcion = domain.descripcion,
            idFinca = domain.idFinca
        )
    }
}
