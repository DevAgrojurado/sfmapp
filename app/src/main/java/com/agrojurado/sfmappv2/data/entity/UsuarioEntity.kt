package com.agrojurado.sfmappv2.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuario",
    indices = [
        Index(value = ["cedula"], unique = true),
        Index(value = ["email"], unique = true),
        Index(value = ["codigo"], unique = true),
        Index(value = ["idcargo"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = CargoEntity::class,
            parentColumns = ["id"],
            childColumns = ["idcargo"],
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
    @ColumnInfo(name = "vigente") val vigente: Int = 0,
    @ColumnInfo(name = "idcargo") val idcargo: Int
)