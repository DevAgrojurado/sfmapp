package com.agrojurado.sfmappv2.domain.usecase.cargo

import com.agrojurado.sfmappv2.domain.model.Cargo
import com.agrojurado.sfmappv2.domain.repository.CargoRepository
import javax.inject.Inject

class DeleteCargoUseCase @Inject constructor(private val repository: CargoRepository) {
    suspend operator fun invoke(cargo: Cargo) = repository.deleteCargo(cargo)
}