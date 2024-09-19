package com.agrojurado.sfmappv2.domain.usecase.cargo

import com.agrojurado.sfmappv2.domain.model.Cargo
import com.agrojurado.sfmappv2.domain.repository.CargoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllCargosUseCase @Inject constructor(private val repository: CargoRepository) {
    operator fun invoke(): Flow<List<Cargo>> = repository.getAllCargos()
}