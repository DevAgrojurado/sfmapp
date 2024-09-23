package com.agrojurado.sfmappv2.domain.usecase.area

import com.agrojurado.sfmappv2.domain.model.Area
import com.agrojurado.sfmappv2.domain.repository.AreaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllAreasUseCase @Inject constructor(private val repository: AreaRepository) {
    operator fun invoke(): Flow<List<Area>> = repository.getAllAreas()
}