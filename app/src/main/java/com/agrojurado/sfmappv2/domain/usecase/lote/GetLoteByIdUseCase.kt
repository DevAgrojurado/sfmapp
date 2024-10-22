package com.agrojurado.sfmappv2.domain.usecase.lote

import com.agrojurado.sfmappv2.domain.model.Cargo
import com.agrojurado.sfmappv2.domain.model.Lote
import com.agrojurado.sfmappv2.domain.repository.CargoRepository
import com.agrojurado.sfmappv2.domain.repository.LoteRepository
import javax.inject.Inject

class GetLoteByIdUseCase @Inject constructor(private val repository: LoteRepository) {
    suspend operator fun invoke(id: Int): Lote? = repository.getLoteById(id)
}