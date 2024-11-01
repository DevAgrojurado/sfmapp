package com.agrojurado.sfmappv2.domain.usecase.sync

import com.agrojurado.sfmappv2.domain.repository.CargoRepository
import javax.inject.Inject

class SyncCargosUseCase @Inject constructor(
    private val cargoRepository: CargoRepository
) {
    suspend operator fun invoke(): Boolean {
        return cargoRepository.fullSync()
    }
}