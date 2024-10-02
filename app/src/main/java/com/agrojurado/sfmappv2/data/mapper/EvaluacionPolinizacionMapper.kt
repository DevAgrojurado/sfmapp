package com.agrojurado.sfmappv2.data.mapper

import com.agrojurado.sfmappv2.data.entity.EvaluacionPolinizacionEntity
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import javax.inject.Inject

object EvaluacionPolinizacionMapper {

    fun toDomain(entity: EvaluacionPolinizacionEntity): EvaluacionPolinizacion {
        return EvaluacionPolinizacion(
            id = entity.id,
            fecha = entity.fecha,
            hora = entity.hora,
            semana = entity.semana,
            idEvaluador = entity.idevaluador,
            codigoEvaluador = entity.codigoevaluador,
            idPolinizador = entity.idpolinizador,
            lote = entity.lote,
            inflorescencia = entity.inflorescencia,
            antesis = entity.antesis,
            postAntesis = entity.postantesis,
            espate = entity.espate,
            aplicacion = entity.aplicacion,
            marcacion = entity.marcacion,
            repaso1 = entity.repaso1,
            repaso2 = entity.repaso2,
            observaciones = entity.observaciones
        )
    }

    fun toEntity(domain: EvaluacionPolinizacion): EvaluacionPolinizacionEntity {
        return EvaluacionPolinizacionEntity(
            id = domain.id,
            fecha = domain.fecha,
            hora = domain.hora,
            semana = domain.semana,
            idevaluador = domain.idEvaluador,
            codigoevaluador = domain.codigoEvaluador,
            idpolinizador = domain.idPolinizador,
            lote = domain.lote,
            inflorescencia = domain.inflorescencia,
            antesis = domain.antesis,
            postantesis = domain.postAntesis,
            espate = domain.espate,
            aplicacion = domain.aplicacion,
            marcacion = domain.marcacion,
            repaso1 = domain.repaso1,
            repaso2 = domain.repaso2,
            observaciones = domain.observaciones
        )
    }
}