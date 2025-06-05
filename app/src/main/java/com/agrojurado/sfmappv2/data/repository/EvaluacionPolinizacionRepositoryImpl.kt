package com.agrojurado.sfmappv2.data.repository

import android.content.Context
import android.util.Log
import com.agrojurado.sfmappv2.data.local.dao.EvaluacionPolinizacionDao
import com.agrojurado.sfmappv2.data.mapper.EvaluacionPolinizacionMapper
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import com.agrojurado.sfmappv2.domain.repository.EvaluacionPolinizacionRepository
import com.agrojurado.sfmappv2.data.remote.dto.common.utils.NetworkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class EvaluacionPolinizacionRepositoryImpl @Inject constructor(
    private val dao: EvaluacionPolinizacionDao,
    @ApplicationContext private val context: Context
) : EvaluacionPolinizacionRepository {

    companion object {
        private const val TAG = "EvaluacionRepository"
    }

    private suspend fun notifyUser(message: String) {
        withContext(Dispatchers.Main) {
            NetworkManager.showToast(context, message)
        }
    }

    override fun getEvaluaciones(): Flow<List<EvaluacionPolinizacion>> {
        return dao.getEvaluaciones()
            .map { entities -> entities.map(EvaluacionPolinizacionMapper::toDomain) }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getEvaluacionById(id: Int): EvaluacionPolinizacion? {
        return dao.getEvaluacionById(id)?.let(EvaluacionPolinizacionMapper::toDomain)
    }

    override suspend fun getLastEvaluacion(): EvaluacionPolinizacion? {
        return dao.getLastEvaluacion()?.let(EvaluacionPolinizacionMapper::toDomain)
    }

    override suspend fun checkPalmExists(semana: Int, lote: Int, palma: Int, idPolinizador: Int, seccion: Int, evaluacionGeneralId: Int): Boolean {
        return dao.checkPalmExists(semana, lote, palma, idPolinizador, seccion, evaluacionGeneralId) > 0
    }

    override suspend fun insertEvaluacion(evaluacion: EvaluacionPolinizacion): Long {
        return withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            val localId = dao.insertEvaluacion(
                EvaluacionPolinizacionMapper.toDatabase(
                    evaluacion.copy(
                        syncStatus = "PENDING",
                        timestamp = timestamp
                    )
                )
            )
            localId
        }
    }

    override suspend fun updateEvaluacion(evaluacion: EvaluacionPolinizacion) {
        withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            val updatedEntity = EvaluacionPolinizacionMapper.toDatabase(evaluacion).copy(
                syncStatus = if (evaluacion.syncStatus != "SYNCED") "PENDING" else "SYNCED",
                timestamp = timestamp
            )
            dao.updateEvaluacion(updatedEntity)
            notifyUser("Actualización guardada localmente")
        }
    }

    override suspend fun deleteEvaluacion(evaluacion: EvaluacionPolinizacion) {
        withContext(Dispatchers.IO) {
            dao.deleteEvaluacion(EvaluacionPolinizacionMapper.toDatabase(evaluacion))
            notifyUser("Eliminada localmente")
        }
    }

    override suspend fun deleteEvaluacionesByEvaluacionGeneralId(evaluacionGeneralId: Int) {
        withContext(Dispatchers.IO) {
            dao.deleteByEvaluacionGeneralId(evaluacionGeneralId)
            Log.d(TAG, "🗑️ Deleted EvaluacionPolinizacion with evaluacionGeneralId: $evaluacionGeneralId")
        }
    }

    override suspend fun associateWithGeneralEvaluation(evaluacionId: Int, evaluacionGeneralId: Int) {
        withContext(Dispatchers.IO) {
            dao.updateEvaluacionGeneralId(evaluacionId, evaluacionGeneralId)
            Log.d(TAG, "Asociada EvaluacionPolinizacion ID $evaluacionId con EvaluacionGeneral ID $evaluacionGeneralId")
        }
    }

    override fun getEvaluacionesByEvaluacionGeneralId(evaluacionGeneralId: Int): Flow<List<EvaluacionPolinizacion>> {
        return dao.getEvaluacionesByEvaluacionGeneralId(evaluacionGeneralId)
            .map { entities -> entities.map(EvaluacionPolinizacionMapper::toDomain) }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getUnsyncedEvaluationsCount(): Int {
        return withContext(Dispatchers.IO) {
            dao.getUnsyncedEvaluationsCount()
        }
    }
}