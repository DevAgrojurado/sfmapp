package com.agrojurado.sfmappv2.data.repository

import com.agrojurado.sfmappv2.data.dao.CargoDao
import com.agrojurado.sfmappv2.data.dao.FincaDao
import com.agrojurado.sfmappv2.data.entity.CargoEntity
import com.agrojurado.sfmappv2.data.mapper.CargoMapper
import com.agrojurado.sfmappv2.data.mapper.FincaMapper
import com.agrojurado.sfmappv2.domain.model.Cargo
import com.agrojurado.sfmappv2.domain.model.Finca
import com.agrojurado.sfmappv2.domain.repository.CargoRepository
import com.agrojurado.sfmappv2.domain.repository.FincaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FincaRepositoryImpl @Inject constructor(
    private val fincaDao: FincaDao
) : FincaRepository {

    override suspend fun insertFinca(finca: Finca): Long {
        return fincaDao.insertFinca(FincaMapper.toDatabase(finca))
    }

    override fun getAllFincas(): Flow<List<Finca>> {
        return fincaDao.getAllFincas().map { entities ->
            entities.map { FincaMapper.toDomain(it) }
        }
    }

    override suspend fun getFincaById(id: Int): Finca? {
        return fincaDao.getFincaById(id)?.let { FincaMapper.toDomain(it) }
    }

    override suspend fun updateFinca(finca: Finca) {
        fincaDao.updateFinca(FincaMapper.toDatabase(finca))
    }

    override suspend fun deleteFinca(finca: Finca) {
        fincaDao.deleteFinca(FincaMapper.toDatabase(finca))
    }

    override suspend fun deleteAllFincas() {
        fincaDao.deleteAllFincas()
    }

    override suspend fun crearFincaPredeterminado() {
        TODO("Not yet implemented")
    }


}
