package com.agrojurado.sfmappv2.domain.repository

import com.agrojurado.sfmappv2.domain.model.EvaluacionGeneral
import kotlinx.coroutines.flow.Flow

interface EvaluacionGeneralRepository {

    // Operaciones CRUD locales
    suspend fun insertEvaluacionGeneral(evaluacionGeneral: EvaluacionGeneral): Long
    suspend fun updateEvaluacionGeneral(evaluacionGeneral: EvaluacionGeneral)
    suspend fun deleteEvaluacionGeneral(evaluacionGeneral: EvaluacionGeneral)
    suspend fun getEvaluacionGeneralById(id: Int): EvaluacionGeneral?
    fun getAllEvaluacionesGenerales(): Flow<List<EvaluacionGeneral>>

    // Operaciones relacionadas con evaluaciones temporales
    suspend fun getActiveTemporaryEvaluacion(): EvaluacionGeneral?
    suspend fun finalizeTemporaryEvaluacion(evaluacionId: Int)
    suspend fun deleteTemporaryEvaluaciones()

    // Operaciones de sincronización
    suspend fun getUnsyncedEvaluationsCount(): Int
    suspend fun syncEvaluacionesGenerales(): Map<Int, Int>
    suspend fun fetchEvaluacionesFromServer() // Obtiene evaluaciones del servidor
}