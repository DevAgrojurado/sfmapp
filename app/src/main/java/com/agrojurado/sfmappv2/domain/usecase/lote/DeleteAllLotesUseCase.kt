package com.agrojurado.sfmappv2.domain.usecase.lote

import com.agrojurado.sfmappv2.domain.repository.LoteRepository
import javax.inject.Inject

class DeleteAllLotesUseCase @Inject constructor(private val repository: LoteRepository) {
    suspend operator fun invoke() = repository.deleteAllLotes()
}