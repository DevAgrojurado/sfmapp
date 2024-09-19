package com.agrojurado.sfmappv2.data.repository

import com.agrojurado.sfmappv2.data.dao.CargoDao
import com.agrojurado.sfmappv2.data.mapper.CargoMapper
import com.agrojurado.sfmappv2.domain.model.Cargo
import com.agrojurado.sfmappv2.domain.repository.CargoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CargoRepositoryImpl @Inject constructor(
    private val cargoDao: CargoDao
) : CargoRepository {

    override suspend fun insertCargo(cargo: Cargo): Long {
        return cargoDao.insertCargo(CargoMapper.mapToEntity(cargo))
    }

    override fun getAllCargos(): Flow<List<Cargo>> {
        return cargoDao.getAllCargos().map { entities ->
            entities.map { CargoMapper.mapToDomain(it) }
        }
    }

    override suspend fun getCargoById(id: Int): Cargo? {
        return cargoDao.getCargoById(id)?.let { CargoMapper.mapToDomain(it) }
    }

    override suspend fun updateCargo(cargo: Cargo) {
        cargoDao.updateCargo(CargoMapper.mapToEntity(cargo))
    }

    override suspend fun deleteCargo(cargo: Cargo) {
        cargoDao.deleteCargo(CargoMapper.mapToEntity(cargo))
    }

    override suspend fun deleteAllCargos() {
        cargoDao.deleteAllCargos()
    }
}