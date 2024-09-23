package com.agrojurado.sfmappv2.data.repository

import com.agrojurado.sfmappv2.data.dao.AreaDao
import com.agrojurado.sfmappv2.data.mapper.AreaMapper
import com.agrojurado.sfmappv2.domain.model.Area
import com.agrojurado.sfmappv2.domain.repository.AreaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AreaRepositoryImpl @Inject constructor(
    private val areaDao: AreaDao
) : AreaRepository {

    override suspend fun insertArea(area: Area): Long {
        return areaDao.insertArea(AreaMapper.toDatabase(area))
    }

    override fun getAllAreas(): Flow<List<Area>> {
        return areaDao.getAllAreas().map { entities ->
            entities.map { AreaMapper.toDomain(it) }
        }
    }

    override suspend fun getAreaById(id: Int): Area? {
        return areaDao.getAreaById(id)?.let { AreaMapper.toDomain(it) }
    }

    override suspend fun updateArea(area: Area) {
        areaDao.updateArea(AreaMapper.toDatabase(area))
    }

    override suspend fun deleteArea(area: Area) {
        areaDao.deleteArea(AreaMapper.toDatabase(area))
    }

    override suspend fun deleteAllAreas() {
        areaDao.deleteAllAreas()
    }
}