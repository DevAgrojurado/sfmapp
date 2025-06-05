package com.agrojurado.sfmappv2.data.repository

import android.content.Context
import android.util.Log
import com.agrojurado.sfmappv2.data.local.dao.EvaluacionGeneralDao
import com.agrojurado.sfmappv2.data.mapper.EvaluacionGeneralMapper
import com.agrojurado.sfmappv2.data.remote.dto.common.utils.NetworkManager
import com.agrojurado.sfmappv2.domain.model.EvaluacionGeneral
import com.agrojurado.sfmappv2.domain.repository.EvaluacionGeneralRepository
import com.agrojurado.sfmappv2.domain.repository.EvaluacionPolinizacionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvaluacionGeneralRepositoryImpl @Inject constructor(
    private val evaluacionGeneralDao: EvaluacionGeneralDao,
    private val evaluacionPolinizacionRepository: EvaluacionPolinizacionRepository,
    @ApplicationContext private val context: Context
) : EvaluacionGeneralRepository {

    companion object {
        private const val TAG = "EvaluacionGeneralRepo"
    }

    private suspend fun notifyUser(message: String) {
        withContext(Dispatchers.Main) {
            NetworkManager.showToast(context, message)
        }
    }

    override suspend fun insertEvaluacionGeneral(evaluacionGeneral: EvaluacionGeneral): Long {
        return withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            val localId = evaluacionGeneralDao.insertEvaluacionGeneral(
                EvaluacionGeneralMapper.toDatabase(
                    evaluacionGeneral.copy(
                        syncStatus = "PENDING",
                        timestamp = timestamp
                    )
                )
            )
            Log.d(TAG, "Inserted EvaluacionGeneral with local ID $localId, isTemporary: ${evaluacionGeneral.isTemporary}")
            localId
        }
    }

    override suspend fun updateEvaluacionGeneral(evaluacionGeneral: EvaluacionGeneral) {
        withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            val updatedEntity = EvaluacionGeneralMapper.toDatabase(
                evaluacionGeneral.copy(
                    syncStatus = if (evaluacionGeneral.syncStatus != "SYNCED") "PENDING" else "SYNCED",
                    timestamp = timestamp
                )
            )
            evaluacionGeneralDao.updateEvaluacionGeneral(updatedEntity)
            notifyUser("Evaluación general actualizada localmente${if (evaluacionGeneral.isTemporary) " (temporal)" else ""}")
        }
    }

    override suspend fun deleteEvaluacionGeneral(evaluacionGeneral: EvaluacionGeneral) {
        withContext(Dispatchers.IO) {
            try {
                evaluacionGeneralDao.deleteEvaluacionGeneral(EvaluacionGeneralMapper.toDatabase(evaluacionGeneral))
                evaluacionPolinizacionRepository.deleteEvaluacionesByEvaluacionGeneralId(evaluacionGeneral.id!!)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting EvaluacionGeneral: ${e.message}", e)
                throw e
            }
        }
    }

    override suspend fun getEvaluacionGeneralById(id: Int): EvaluacionGeneral? {
        return evaluacionGeneralDao.getEvaluacionGeneralById(id)?.let {
            EvaluacionGeneralMapper.toDomain(it)
        }
    }

    override fun getAllEvaluacionesGenerales(): Flow<List<EvaluacionGeneral>> {
        return evaluacionGeneralDao.getAllEvaluacionesGenerales()
            .map { entities -> entities.map { EvaluacionGeneralMapper.toDomain(it) } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getActiveTemporaryEvaluacion(): EvaluacionGeneral? {
        return evaluacionGeneralDao.getTemporalEvaluacionGeneral()?.let {
            EvaluacionGeneralMapper.toDomain(it)
        }
    }

    override suspend fun finalizeTemporaryEvaluacion(evaluacionId: Int) {
        withContext(Dispatchers.IO) {
            val evaluacion = evaluacionGeneralDao.getEvaluacionGeneralById(evaluacionId)
            evaluacion?.let {
                val updated = EvaluacionGeneralMapper.toDomain(it).copy(isTemporary = false, syncStatus = "PENDING")
                updateEvaluacionGeneral(updated)
                Log.d(TAG, "Finalized temporary EvaluacionGeneral ID $evaluacionId")
            } ?: Log.w(TAG, "No temporary EvaluacionGeneral found with ID $evaluacionId")
        }
    }

    override suspend fun deleteTemporaryEvaluaciones() {
        withContext(Dispatchers.IO) {
            evaluacionGeneralDao.deleteTemporalEvaluacionGeneral()
            Log.d(TAG, "Deleted all temporary EvaluacionesGenerales")
        }
    }

    override suspend fun getLatestTemporaryEvaluationId(): Int? {
        return evaluacionGeneralDao.getLatestTemporaryEvaluationId()
    }

    override suspend fun getUnsyncedEvaluationsCount(): Int {
        return evaluacionGeneralDao.getUnsyncedEvaluationsCount()
    }
}