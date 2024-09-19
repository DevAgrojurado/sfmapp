package com.agrojurado.sfmappv2.domain.repository

import com.agrojurado.sfmappv2.domain.model.Cargo
import kotlinx.coroutines.flow.Flow

interface CargoRepository {
    suspend fun insertCargo(cargo: Cargo): Long

    fun getAllCargos(): Flow<List<Cargo>>

    suspend fun getCargoById(id: Int): Cargo?

    suspend fun updateCargo(cargo: Cargo)

    suspend fun deleteCargo(cargo: Cargo)

    suspend fun deleteAllCargos()
}