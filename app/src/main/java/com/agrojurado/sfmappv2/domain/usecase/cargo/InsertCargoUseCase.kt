package com.agrojurado.sfmappv2.domain.usecase.cargo

import com.agrojurado.sfmappv2.domain.model.Cargo
import com.agrojurado.sfmappv2.domain.repository.CargoRepository
import javax.inject.Inject

class InsertCargoUseCase @Inject constructor(private val repository: CargoRepository) {
    suspend operator fun invoke(cargo: Cargo): Long = repository.insertCargo(cargo)
}