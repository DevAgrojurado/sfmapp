package com.agrojurado.sfmappv2.domain.usecase.lote

import com.agrojurado.sfmappv2.domain.model.Lote
import com.agrojurado.sfmappv2.domain.repository.LoteRepository
import javax.inject.Inject

class InsertLoteUseCase @Inject constructor(private val repository: LoteRepository) {
    suspend operator fun invoke(lote: Lote): Long = repository.insertLote(lote)
}