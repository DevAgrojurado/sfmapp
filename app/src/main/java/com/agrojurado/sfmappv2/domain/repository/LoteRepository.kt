package com.agrojurado.sfmappv2.domain.repository

import com.agrojurado.sfmappv2.domain.model.Cargo
import com.agrojurado.sfmappv2.domain.model.Lote
import kotlinx.coroutines.flow.Flow

interface LoteRepository {
    suspend fun insertLote(lote: Lote): Long

    fun getAllLotes(): Flow<List<Lote>>

    suspend fun getLoteById(id: Int): Lote?

    suspend fun updateLote(cargo: Lote)

    suspend fun deleteLote(cargo: Lote)

    suspend fun deleteAllLotes()

    suspend fun syncLotes()

    suspend fun fullSync(): Boolean


}