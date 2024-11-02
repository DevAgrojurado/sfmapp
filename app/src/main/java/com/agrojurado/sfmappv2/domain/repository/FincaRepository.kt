package com.agrojurado.sfmappv2.domain.repository

import com.agrojurado.sfmappv2.domain.model.Cargo
import com.agrojurado.sfmappv2.domain.model.Finca
import kotlinx.coroutines.flow.Flow

interface FincaRepository {

    suspend fun insertFinca(finca: Finca): Long

    fun getAllFincas(): Flow<List<Finca>>

    suspend fun getFincaById(id: Int): Finca?

    suspend fun updateFinca(finca: Finca)

    suspend fun deleteFinca(finca: Finca)

    suspend fun deleteAllFincas()

    suspend fun fullSync(): Boolean

    suspend fun syncFincas()

}