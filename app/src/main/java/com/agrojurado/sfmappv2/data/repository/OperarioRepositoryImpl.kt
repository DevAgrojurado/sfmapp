package com.agrojurado.sfmappv2.data.repository

import com.agrojurado.sfmappv2.data.dao.OperarioDao
import com.agrojurado.sfmappv2.data.mapper.OperarioMapper
import com.agrojurado.sfmappv2.domain.model.Operario
import com.agrojurado.sfmappv2.domain.repository.OperarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OperarioRepositoryImpl @Inject constructor(
    private val operarioDao: OperarioDao
        ) : OperarioRepository {
    override suspend fun insertOperario(operario: Operario): Long {
        return operarioDao.insertOperario(OperarioMapper.toDatabase(operario))
    }

    override fun getAllOperarios(): Flow<List<Operario>> {
        return operarioDao.getAllOperarios().map { entities ->
            entities.map { OperarioMapper.toDomain(it) }
        }
    }

    override suspend fun getOperarioById(id: Int): Operario? {
        return operarioDao.getOperarioById(id)?.let { OperarioMapper.toDomain(it) }
    }

    override suspend fun updateOperario(operario: Operario) {
        operarioDao.updateOperario(OperarioMapper.toDatabase(operario))
    }

    override suspend fun deleteOperario(operario: Operario) {
        operarioDao.deleteOperario(OperarioMapper.toDatabase(operario))
    }

    override suspend fun deleteAllOperarios() {
        operarioDao.deleteAllOperarios()
    }

    override fun searchOperarios(query: String): Flow<List<Operario>> {
        return operarioDao.searchOperarios(query).map { list ->
            list.map { OperarioMapper.toDomain(it) }
        }
    }

    fun getAllOperariosUseCase(): Flow<List<Operario>> {
        return operarioDao.getAllOperarios().map { list ->
            list?.map { OperarioMapper.toDomain(it) } ?: emptyList()
        }
    }

}