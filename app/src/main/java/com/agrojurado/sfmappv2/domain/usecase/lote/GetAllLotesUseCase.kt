package com.agrojurado.sfmappv2.domain.usecase.lote

import com.agrojurado.sfmappv2.domain.model.Lote
import com.agrojurado.sfmappv2.domain.repository.LoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllLotesUseCase @Inject constructor(private val repository: LoteRepository) {
    operator fun invoke(): Flow<List<Lote>> = repository.getAllLotes()
}