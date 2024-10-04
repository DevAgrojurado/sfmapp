package com.agrojurado.sfmappv2.domain.repository

import com.agrojurado.sfmappv2.domain.model.Operario
import kotlinx.coroutines.flow.Flow

interface OperarioRepository {

    suspend fun insertOperario(operario: Operario): Long

    fun getAllOperarios(): Flow<List<Operario>>

    suspend fun getOperarioById(id: Int): Operario?

    suspend fun updateOperario(operario: Operario)

    suspend fun deleteOperario(operario: Operario)

    suspend fun deleteAllOperarios()

    fun searchOperarios(query: String): Flow<List<Operario>>

}