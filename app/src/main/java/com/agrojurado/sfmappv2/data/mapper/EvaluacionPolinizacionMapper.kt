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
            idPolinizador = entity.idpolinizador,
            lote = entity.lote,
            seccion = entity.seccion,
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
            idpolinizador = domain.idPolinizador,
            lote = domain.lote,
            seccion = domain.seccion,
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
            observaciones = domain.observaciones
        )
    }
}