package com.agrojurado.sfmappv2.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "lote",
    indices = [
        Index(value = ["idFinca"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = FincaEntity::class,
            parentColumns = ["id"],
            childColumns = ["idFinca"],
            onDelete = ForeignKey.NO_ACTION
        ),
    ]
)
data class LoteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") var id: Int = 0,
    @ColumnInfo(name = "descripcion") var descripcion: String?,
    @ColumnInfo(name = "idFinca") var idFinca: Int?,
    @ColumnInfo(name = "isSynced") var isSynced: Boolean = false
)
