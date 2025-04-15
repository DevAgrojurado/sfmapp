package com.agrojurado.sfmappv2.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.agrojurado.sfmappv2.data.local.entity.EvaluacionGeneralEntity
import com.agrojurado.sfmappv2.data.local.entity.EvaluacionPolinizacionEntity

data class EvaluacionGeneralWithEvaluaciones(
    @Embedded val evaluacionGeneral: EvaluacionGeneralEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "evaluacionGeneralId"
    )
    val evaluaciones: List<EvaluacionPolinizacionEntity>
)