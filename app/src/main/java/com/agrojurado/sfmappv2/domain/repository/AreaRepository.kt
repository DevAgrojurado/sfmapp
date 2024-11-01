package com.agrojurado.sfmappv2.domain.repository

import com.agrojurado.sfmappv2.domain.model.Area
import kotlinx.coroutines.flow.Flow

interface AreaRepository {
    suspend fun insertArea(area: Area): Long

    fun getAllAreas(): Flow<List<Area>>

    suspend fun getAreaById(id: Int): Area?

    suspend fun updateArea(area: Area)

    suspend fun deleteArea(area: Area)

    suspend fun deleteAllAreas()

    suspend fun fullSync(): Boolean

    suspend fun syncAreas()

}