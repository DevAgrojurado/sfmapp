package com.agrojurado.sfmappv2.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "evaluaciongeneral",
    indices = [
        Index(value = ["idevaluadorev"]),
        Index(value = ["idpolinizadorev"]),
        Index(value = ["serverId"]),
        Index(value = ["idloteev"])
    ],
    foreignKeys = [
        ForeignKey(entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["idevaluadorev"],
            onDelete = ForeignKey.NO_ACTION),

        ForeignKey(entity = OperarioEntity::class, parentColumns = ["id"],
            childColumns = ["idpolinizadorev"],
            onDelete = ForeignKey.NO_ACTION),

        ForeignKey(entity = LoteEntity::class, parentColumns = ["id"],
            childColumns = ["idloteev"],
            onDelete = ForeignKey.NO_ACTION)
    ]
)
data class EvaluacionGeneralEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") var id: Int = 0,
    @ColumnInfo(name = "serverId") var serverId: Int?,
    @ColumnInfo(name = "fecha") val fecha: String,
    @ColumnInfo(name = "hora") val hora: String,
    @ColumnInfo(name = "semana") val semana: Int,
    @ColumnInfo(name = "idevaluadorev") val idevaluadorev: Int,
    @ColumnInfo(name = "idpolinizadorev") val idpolinizadorev: Int?,
    @ColumnInfo(name = "idloteev") val idLoteev: Int?,
    @ColumnInfo(name = "isSynced") var isSynced: Boolean = false,
    @ColumnInfo(name = "isTemporary") var isTemporary: Boolean = false,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "fotoPath") var fotoPath: String? = null,
    @ColumnInfo(name = "firmaPath") var firmaPath: String? = null
)