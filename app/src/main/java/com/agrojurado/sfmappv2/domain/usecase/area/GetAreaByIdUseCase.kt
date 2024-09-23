package com.agrojurado.sfmappv2.domain.usecase.area

import com.agrojurado.sfmappv2.domain.model.Area
import com.agrojurado.sfmappv2.domain.repository.AreaRepository
import javax.inject.Inject

class GetAreaByIdUseCase @Inject constructor(private val repository: AreaRepository) {
    suspend operator fun invoke(id: Int): Area? = repository.getAreaById(id)
}