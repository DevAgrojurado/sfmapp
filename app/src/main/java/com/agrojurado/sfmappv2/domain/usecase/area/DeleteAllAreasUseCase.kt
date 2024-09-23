package com.agrojurado.sfmappv2.domain.usecase.area

import com.agrojurado.sfmappv2.domain.repository.AreaRepository
import javax.inject.Inject

class DeleteAllAreasUseCase @Inject constructor(private val repository: AreaRepository) {
    suspend operator fun invoke() = repository.deleteAllAreas()
}