package com.agrojurado.sfmappv2.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.agrojurado.sfmappv2.data.local.entity.EvaluacionGeneralEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EvaluacionGeneralDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluacionGeneral(evaluacionGeneral: EvaluacionGeneralEntity): Long

    @Update
    suspend fun updateEvaluacionGeneral(evaluacionGeneral: EvaluacionGeneralEntity)

    @Delete
    suspend fun deleteEvaluacionGeneral(evaluacionGeneral: EvaluacionGeneralEntity)

    @Query("SELECT * FROM evaluaciongeneral WHERE isTemporary = 1 LIMIT 1")
    suspend fun getTemporalEvaluacionGeneral(): EvaluacionGeneralEntity?

    @Query("DELETE FROM evaluaciongeneral WHERE isTemporary = 1")
    suspend fun deleteTemporalEvaluacionGeneral()

    @Query("SELECT * FROM evaluaciongeneral WHERE id = :id")
    suspend fun getEvaluacionGeneralById(id: Int): EvaluacionGeneralEntity?

    @Query("SELECT * FROM evaluaciongeneral")
    fun getAllEvaluacionesGenerales(): Flow<List<EvaluacionGeneralEntity>>

    @Query("SELECT * FROM evaluaciongeneral WHERE syncStatus IN ('PENDING', 'FAILED') AND isTemporary = 0")
    suspend fun getUnsyncedEvaluaciones(): List<EvaluacionGeneralEntity>

    @Query("SELECT MAX(timestamp) FROM evaluaciongeneral WHERE syncStatus = 'SYNCED'")
    suspend fun getLastSyncTimestamp(): Long?

    @Query("SELECT * FROM evaluaciongeneral WHERE serverId = :serverId LIMIT 1")
    suspend fun getEvaluacionGeneralByServerId(serverId: Int): EvaluacionGeneralEntity?

    @Query("SELECT COUNT(*) FROM evaluaciongeneral WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getUnsyncedEvaluationsCount(): Int

    @Query("SELECT * FROM evaluaciongeneral WHERE syncStatus = 'SYNCED' AND (fotoPath IS NOT NULL AND fotoPath NOT LIKE 'http%' OR firmaPath IS NOT NULL AND firmaPath NOT LIKE 'http%')")
    fun getSyncedEvaluacionesWithLocalPaths(): List<EvaluacionGeneralEntity>

    @Query("SELECT id FROM evaluaciongeneral WHERE isTemporary = 1 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestTemporaryEvaluationId(): Int?

    @Transaction
    suspend fun transaction(block: suspend () -> Unit) = block()

}