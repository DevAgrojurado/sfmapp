package com.agrojurado.sfmappv2.data.repository

import com.agrojurado.sfmappv2.data.dao.EvaluacionPolinizacionDao
import com.agrojurado.sfmappv2.data.mapper.EvaluacionPolinizacionMapper
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import com.agrojurado.sfmappv2.domain.repository.EvaluacionPolinizacionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class EvaluacionPolinizacionRepositoryImpl @Inject constructor(
    private val dao: EvaluacionPolinizacionDao,
    private val mapper: EvaluacionPolinizacionMapper
) : EvaluacionPolinizacionRepository {

    override suspend fun insertEvaluacion(evaluacion: EvaluacionPolinizacion): Long {
        return dao.insertEvaluacion(mapper.toEntity(evaluacion))
    }

    override suspend fun updateEvaluacion(evaluacion: EvaluacionPolinizacion) {
        dao.updateEvaluacion(mapper.toEntity(evaluacion))
    }

    override suspend fun deleteEvaluacion(evaluacion: EvaluacionPolinizacion) {
        dao.deleteEvaluacion(mapper.toEntity(evaluacion))
    }

    override fun getEvaluaciones(): Flow<List<EvaluacionPolinizacion>> {
        return dao.getEvaluaciones().map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override suspend fun getEvaluacionById(id: Long): EvaluacionPolinizacion? {
        return dao.getEvaluacionById(id)?.let { mapper.toDomain(it) }
    }
}