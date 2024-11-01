package com.agrojurado.sfmappv2.domain.usecase.sync

import com.agrojurado.sfmappv2.domain.repository.AreaRepository
import javax.inject.Inject

class SyncAreasUseCase @Inject constructor(
    private val areaRepository: AreaRepository
) {
    suspend operator fun invoke(): Boolean {
        return areaRepository.fullSync()
    }
}