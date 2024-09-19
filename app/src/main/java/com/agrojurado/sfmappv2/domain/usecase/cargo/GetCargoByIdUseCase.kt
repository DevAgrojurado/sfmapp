package com.agrojurado.sfmappv2.domain.usecase.cargo

import com.agrojurado.sfmappv2.domain.model.Cargo
import com.agrojurado.sfmappv2.domain.repository.CargoRepository
import javax.inject.Inject

class GetCargoByIdUseCase @Inject constructor(private val repository: CargoRepository) {
    suspend operator fun invoke(id: Int): Cargo? = repository.getCargoById(id)
}