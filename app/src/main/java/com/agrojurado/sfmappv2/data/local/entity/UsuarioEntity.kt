package com.agrojurado.sfmappv2.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuario",
        indices = [
            Index(value = ["idCargo"]),
            Index(value = ["idArea"])

    ],
    foreignKeys = [
        ForeignKey(
        entity = CargoEntity::class,
        parentColumns = ["id"],
        childColumns = ["idCargo"],
        onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = OperarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["idArea"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = OperarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["idFinca"],
            onDelete = ForeignKey.NO_ACTION
        )
    ]
)
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Int = 0,
    @ColumnInfo(name = "codigo") val codigo: String,
    @ColumnInfo(name = "nombre") val nombre: String,
    @ColumnInfo(name = "cedula") val cedula: String,
    @ColumnInfo(name = "email") val email: String,
    @ColumnInfo(name = "clave") val clave: String,
    @ColumnInfo(name = "idCargo") val idCargo: Int?,
    @ColumnInfo(name = "idArea") val idArea: Int?,
    @ColumnInfo(name = "idFinca") val idFinca: Int?,
    @ColumnInfo(name = "vigente") val vigente: Int = 0
)