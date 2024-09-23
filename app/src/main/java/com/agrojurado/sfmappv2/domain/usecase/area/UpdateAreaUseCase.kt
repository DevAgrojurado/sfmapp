package com.agrojurado.sfmappv2.domain.usecase.area

import com.agrojurado.sfmappv2.domain.model.Area
import com.agrojurado.sfmappv2.domain.repository.AreaRepository
import javax.inject.Inject

class UpdateAreaUseCase @Inject constructor(private val repository: AreaRepository) {
    suspend operator fun invoke(area: Area) = repository.updateArea(area)
}