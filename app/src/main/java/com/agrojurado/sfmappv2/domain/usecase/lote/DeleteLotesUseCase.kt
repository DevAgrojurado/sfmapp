package com.agrojurado.sfmappv2.domain.usecase.lote

import com.agrojurado.sfmappv2.domain.model.Lote
import com.agrojurado.sfmappv2.domain.repository.LoteRepository
import javax.inject.Inject

class DeleteLotesUseCase @Inject constructor(private val repository: LoteRepository) {
    suspend operator fun invoke(lote: Lote) = repository.deleteLote(lote)
}