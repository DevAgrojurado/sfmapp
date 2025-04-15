package com.agrojurado.sfmappv2.data.mapper

import com.agrojurado.sfmappv2.data.local.entity.EvaluacionGeneralEntity
import com.agrojurado.sfmappv2.data.remote.dto.evaluaciongeneral.EvaluacionGeneralRequest
import com.agrojurado.sfmappv2.data.remote.dto.evaluaciongeneral.EvaluacionGeneralResponse
import com.agrojurado.sfmappv2.domain.model.EvaluacionGeneral

object EvaluacionGeneralMapper {

    fun toDomain(entity: EvaluacionGeneralEntity): EvaluacionGeneral {
        return EvaluacionGeneral(
            id = entity.id,
            serverId = entity.serverId,
            fecha = entity.fecha,
            hora = entity.hora,
            semana = entity.semana,
            idevaluadorev = entity.idevaluadorev,
            idpolinizadorev = entity.idpolinizadorev,
            idLoteev = entity.idLoteev,
            timestamp = entity.timestamp,
            isTemporary = entity.isTemporary,
            isSynced = entity.isSynced,
            fotoPath = entity.fotoPath,
            firmaPath = entity.firmaPath
        )
    }

    fun toDatabase(domain: EvaluacionGeneral): EvaluacionGeneralEntity {
        return EvaluacionGeneralEntity(
            id = domain.id ?: 0,
            serverId = domain.serverId,
            fecha = domain.fecha,
            hora = domain.hora,
            semana = domain.semana,
            idevaluadorev = domain.idevaluadorev,
            idpolinizadorev = domain.idpolinizadorev,
            idLoteev = domain.idLoteev,
            timestamp = domain.timestamp,
            isTemporary = domain.isTemporary,
            isSynced = domain.isSynced,
            fotoPath = domain.fotoPath,
            firmaPath = domain.firmaPath
        )
    }

    fun fromResponse(response: EvaluacionGeneralResponse): EvaluacionGeneral {
        return EvaluacionGeneral(
            id = 0,
            serverId = response.id,
            fecha = response.fecha,
            hora = response.hora,
            semana = response.semana,
            idevaluadorev = response.idevaluadorev,
            idpolinizadorev = response.idpolinizadorev,
            idLoteev = response.idloteev,
            timestamp = response.timestamp,
            fotoPath = response.fotopath,
            firmaPath = response.firmapath
        )
    }

    fun toRequest(domain: EvaluacionGeneral): EvaluacionGeneralRequest {
        return EvaluacionGeneralRequest(
            id = domain.serverId,
            fecha = domain.fecha,
            hora = domain.hora,
            semana = domain.semana,
            idevaluadorev = domain.idevaluadorev,
            idpolinizadorev = domain.idpolinizadorev,
            idloteev = domain.idLoteev,
            fotopath = domain.fotoPath,
            firmapath = domain.firmaPath,
            timestamp = domain.timestamp
        )
    }
}