package com.agrojurado.sfmappv2.data.repository
import com.agrojurado.sfmappv2.data.dao.LoteDao
import com.agrojurado.sfmappv2.data.mapper.LoteMapper
import com.agrojurado.sfmappv2.domain.model.Lote
import com.agrojurado.sfmappv2.domain.repository.LoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LoteRepositoryImpl @Inject constructor(
    private val loteDao: LoteDao
) : LoteRepository {

    override suspend fun insertLote(lote: Lote): Long {
        return loteDao.insertLote(LoteMapper.toDatabase(lote))
    }

    override fun getAllLotes(): Flow<List<Lote>> {
        return loteDao.getAllLotes().map { entities ->
            entities.map { LoteMapper.toDomain(it) }
        }
    }

    override suspend fun getLoteById(id: Int): Lote? {
        return loteDao.getLoteById(id)?.let { LoteMapper.toDomain(it) }
    }

    override suspend fun updateLote(lote: Lote) {
        loteDao.updateLote(LoteMapper.toDatabase(lote))
    }

    override suspend fun deleteLote(lote: Lote) {
        loteDao.deleteLote(LoteMapper.toDatabase(lote))
    }

    override suspend fun deleteAllLotes() {
        loteDao.deleteAllLotes()
    }
}
