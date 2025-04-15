package com.agrojurado.sfmappv2.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.agrojurado.sfmappv2.data.local.entity.EvaluacionGeneralEntity
import com.agrojurado.sfmappv2.data.local.relation.EvaluacionGeneralWithEvaluaciones
import kotlinx.coroutines.flow.Flow

@Dao
interface EvaluacionGeneralDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluacionGeneral(evaluacionGeneral: EvaluacionGeneralEntity): Long

    @Update
    suspend fun updateEvaluacionGeneral(evaluacionGeneral: EvaluacionGeneralEntity)

    @Delete
    suspend fun deleteEvaluacionGeneral(evaluacionGeneral: EvaluacionGeneralEntity)

    @Transaction
    @Query("SELECT * FROM evaluaciongeneral WHERE id = :id")
    fun getEvaluacionGeneralWithEvaluaciones(id: Int): Flow<EvaluacionGeneralWithEvaluaciones>

    @Transaction
    @Query("SELECT * FROM evaluaciongeneral")
    fun getAllEvaluacionesGeneralesWithEvaluaciones(): Flow<List<EvaluacionGeneralWithEvaluaciones>>

    @Query("SELECT * FROM evaluaciongeneral WHERE isTemporary = 1 LIMIT 1")
    suspend fun getTemporalEvaluacionGeneral(): EvaluacionGeneralEntity?

    @Query("DELETE FROM evaluaciongeneral WHERE isTemporary = 1")
    suspend fun deleteTemporalEvaluacionGeneral()

    @Query("SELECT * FROM evaluaciongeneral WHERE id = :id")
    suspend fun getEvaluacionGeneralById(id: Int): EvaluacionGeneralEntity?

    @Query("SELECT * FROM evaluaciongeneral")
    fun getAllEvaluacionesGenerales(): Flow<List<EvaluacionGeneralEntity>>

    @Query("SELECT * FROM evaluaciongeneral WHERE isSynced = 0 AND isTemporary = 0")
    suspend fun getUnsyncedEvaluaciones(): List<EvaluacionGeneralEntity>

    @Query("SELECT MAX(timestamp) FROM evaluaciongeneral WHERE isSynced = 1")
    suspend fun getLastSyncTimestamp(): Long?

    @Query("SELECT * FROM evaluaciongeneral WHERE serverId = :serverId LIMIT 1")
    suspend fun getEvaluacionGeneralByServerId(serverId: Int): EvaluacionGeneralEntity?

    @Query("SELECT COUNT(*) FROM evaluaciongeneral WHERE isSynced = 0")
    suspend fun getUnsyncedEvaluationsCount(): Int

    @Query("SELECT * FROM evaluaciongeneral WHERE isSynced = 0")
    suspend fun getUnsyncedEvaluacionesGenerales(): List<EvaluacionGeneralEntity>

    @Query("SELECT * FROM evaluaciongeneral WHERE fecha = :fecha AND hora = :hora AND idevaluadorev = :idevaluadorev AND idpolinizadorev = :idpolinizadorev AND idLoteev = :idLoteev LIMIT 1")
    suspend fun getEvaluacionGeneralByUniqueFields(
        fecha: String,
        hora: String,
        idevaluadorev: Int,
        idpolinizadorev: Int?,
        idLoteev: Int?
    ): EvaluacionGeneralEntity?

    @Transaction
    suspend fun transaction(block: suspend () -> Unit) = block()
}