package com.agrojurado.sfmappv2.data.mapper

import com.agrojurado.sfmappv2.data.local.entity.EvaluacionPolinizacionEntity
import com.agrojurado.sfmappv2.data.remote.dto.evaluacion.EvaluacionRequest
import com.agrojurado.sfmappv2.data.remote.dto.evaluacion.EvaluacionResponse
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion

object EvaluacionPolinizacionMapper {

    fun toDomain(entity: EvaluacionPolinizacionEntity): EvaluacionPolinizacion {
        return EvaluacionPolinizacion(
            id = entity.id,
            serverId = entity.serverId,
            fecha = entity.fecha,
            hora = entity.hora,
            semana = entity.semana,
            ubicacion = entity.ubicacion,
            idEvaluador = entity.idEvaluador,
            idPolinizador = entity.idPolinizador,
            idlote = entity.idlote,
            seccion = entity.seccion,
            palma = entity.palma,
            inflorescencia = entity.inflorescencia,
            antesis = entity.antesis,
            antesisDejadas = entity.antesisDejadas,
            postAntesis = entity.postantesis,
            postAntesisDejadas = entity.postantesisDejadas,
            espate = entity.espate,
            aplicacion = entity.aplicacion,
            marcacion = entity.marcacion,
            repaso1 = entity.repaso1,
            repaso2 = entity.repaso2,
            observaciones = entity.observaciones,
            evaluacionGeneralId = entity.evaluacionGeneralId,
            timestamp = entity.timestamp,
            syncStatus = entity.syncStatus
        )
    }

    fun toDatabase(domain: EvaluacionPolinizacion): EvaluacionPolinizacionEntity {
        return EvaluacionPolinizacionEntity(
            id = domain.id ?: 0,
            serverId = domain.serverId,
            fecha = domain.fecha,
            hora = domain.hora,
            semana = domain.semana,
            ubicacion = domain.ubicacion,
            idEvaluador = domain.idEvaluador,
            idPolinizador = domain.idPolinizador,
            idlote = domain.idlote,
            seccion = domain.seccion,
            palma = domain.palma,
            inflorescencia = domain.inflorescencia,
            antesis = domain.antesis,
            antesisDejadas = domain.antesisDejadas,
            postantesis = domain.postAntesis,
            postantesisDejadas = domain.postAntesisDejadas,
            espate = domain.espate,
            aplicacion = domain.aplicacion,
            marcacion = domain.marcacion,
            repaso1 = domain.repaso1,
            repaso2 = domain.repaso2,
            observaciones = domain.observaciones,
            evaluacionGeneralId = domain.evaluacionGeneralId,
            timestamp = domain.timestamp,
            syncStatus = domain.syncStatus
        )
    }

    fun fromResponse(response: EvaluacionResponse): EvaluacionPolinizacion {
        return EvaluacionPolinizacion(
            id = 0, // El id local se generará automáticamente
            serverId = response.id, // Guardamos el id del servidor en serverId
            evaluacionGeneralId = null, // Se asignará en DataSyncManager
            fecha = response.fecha,
            hora = response.hora,
            semana = response.semana,
            ubicacion = response.ubicacion,
            idEvaluador = response.idevaluador,
            idPolinizador = response.idpolinizador,
            idlote = response.idlote,
            seccion = response.seccion,
            palma = response.palma,
            inflorescencia = response.inflorescencia,
            antesis = response.antesis,
            antesisDejadas = response.antesisDejadas,
            postAntesis = response.postantesis,
            postAntesisDejadas = response.postantesisDejadas,
            espate = response.espate,
            aplicacion = response.aplicacion,
            marcacion = response.marcacion,
            repaso1 = response.repaso1,
            repaso2 = response.repaso2,
            observaciones = response.observaciones,
            timestamp = response.timestamp,
            syncStatus = "SYNCED"
        )
    }

    fun toRequest(domain: EvaluacionPolinizacion): EvaluacionRequest {
        return EvaluacionRequest(
            id = domain.serverId, // Usamos el serverId para comunicación con el servidor
            evaluaciongeneralid = domain.evaluacionGeneralId,
            fecha = domain.fecha,
            hora = domain.hora,
            semana = domain.semana,
            ubicacion = domain.ubicacion,
            idevaluador = domain.idEvaluador,
            idpolinizador = domain.idPolinizador,
            idlote = domain.idlote,
            seccion = domain.seccion,
            palma = domain.palma,
            inflorescencia = domain.inflorescencia,
            antesis = domain.antesis,
            antesisDejadas = domain.antesisDejadas,
            postantesis = domain.postAntesis,
            postantesisDejadas = domain.postAntesisDejadas,
            espate = domain.espate,
            aplicacion = domain.aplicacion,
            marcacion = domain.marcacion,
            repaso1 = domain.repaso1,
            repaso2 = domain.repaso2,
            observaciones = domain.observaciones,
            timestamp = domain.timestamp
        )
    }
}