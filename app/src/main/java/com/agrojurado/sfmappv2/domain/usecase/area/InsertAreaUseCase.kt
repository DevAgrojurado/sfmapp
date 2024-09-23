package com.agrojurado.sfmappv2.domain.usecase.area

import com.agrojurado.sfmappv2.domain.model.Area
import com.agrojurado.sfmappv2.domain.repository.AreaRepository
import javax.inject.Inject

class InsertAreaUseCase @Inject constructor(private val repository: AreaRepository) {
    suspend operator fun invoke(area: Area): Long = repository.insertArea(area)
}