package com.agrojurado.sfmappv2.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "evaluacionpolinizacion",
    indices = [
        Index(value = ["idevaluador"]),
        Index(value = ["idpolinizador"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["idevaluador"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = OperarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["idpolinizador"],
            onDelete = ForeignKey.NO_ACTION
        )
    ]
)
data class EvaluacionPolinizacionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "fecha") val fecha: String,
    @ColumnInfo(name = "hora") val hora: String,
    @ColumnInfo(name = "semana") val semana: Int,
    @ColumnInfo(name = "ubicacion") val ubicacion: String,
    @ColumnInfo(name = "idevaluador") val idevaluador: Int,
    @ColumnInfo(name = "idpolinizador") val idpolinizador: Int,
    @ColumnInfo(name = "lote") val lote: Int,
    @ColumnInfo(name = "seccion") val seccion: Int,
    @ColumnInfo(name = "palma") val palma: Int?,
    @ColumnInfo(name = "inflorescencia") val inflorescencia: Int?,
    @ColumnInfo(name = "antesis") val antesis: Int?,
    @ColumnInfo(name = "antesisDejadas") val antesisDejadas: Int?,
    @ColumnInfo(name = "postantesisDejadas") val postantesisDejadas: Int?,
    @ColumnInfo(name = "postantesis") val postantesis: Int?,
    @ColumnInfo(name = "espate") val espate: Int?,
    @ColumnInfo(name = "aplicacion") val aplicacion: Int?,
    @ColumnInfo(name = "marcacion") val marcacion: Int?,
    @ColumnInfo(name = "repaso1") val repaso1: Int?,
    @ColumnInfo(name = "repaso2") val repaso2: Int?,
    @ColumnInfo(name = "observaciones") val observaciones: String
)