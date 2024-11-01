package com.agrojurado.sfmappv2.data.repository

import com.agrojurado.sfmappv2.data.local.dao.EvaluacionPolinizacionDao
import com.agrojurado.sfmappv2.data.mapper.EvaluacionPolinizacionMapper
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import com.agrojurado.sfmappv2.domain.repository.EvaluacionPolinizacionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class EvaluacionPolinizacionRepositoryImpl @Inject constructor(
    private val dao: EvaluacionPolinizacionDao
) : EvaluacionPolinizacionRepository {

    override suspend fun insertEvaluacion(evaluacion: EvaluacionPolinizacion): Long {
        return dao.insertEvaluacion(EvaluacionPolinizacionMapper.toEntity(evaluacion))
    }

    override suspend fun updateEvaluacion(evaluacion: EvaluacionPolinizacion) {
        dao.updateEvaluacion(EvaluacionPolinizacionMapper.toEntity(evaluacion))
    }

    override suspend fun deleteEvaluacion(evaluacion: EvaluacionPolinizacion) {
        dao.deleteEvaluacion(EvaluacionPolinizacionMapper.toEntity(evaluacion))
    }

    override fun getEvaluaciones(): Flow<List<EvaluacionPolinizacion>> {
        return dao.getEvaluaciones().map { entities ->
            entities.map { EvaluacionPolinizacionMapper.toDomain(it) }
        }
    }

    override suspend fun getEvaluacionById(id: Long): EvaluacionPolinizacion? {
        return dao.getEvaluacionById(id)?.let { EvaluacionPolinizacionMapper.toDomain(it) }
    }

    override suspend fun getLastEvaluacion(): EvaluacionPolinizacion? {
        return dao.getLastEvaluacion()?.let { EvaluacionPolinizacionMapper.toDomain(it) }
    }

    override suspend fun checkPalmExists(semana: Int, lote: Int, palma: Int, idPolinizador: Int): Boolean {
        return dao.checkPalmExists(semana, lote, palma, idPolinizador) > 0
    }
}
