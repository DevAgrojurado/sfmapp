package com.agrojurado.sfmappv2.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "operario",
    foreignKeys = [ForeignKey(
        entity = CargoEntity::class,
        parentColumns = ["id"],
        childColumns = ["cargoId"],
        onDelete = ForeignKey.NO_ACTION
    )],
    indices = [Index(value = ["cargoId"])]
)
data class OperarioEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Int = 0,
    @ColumnInfo(name = "codigo") var codigo: String,
    @ColumnInfo(name = "nombre") var nombre: String,
    @ColumnInfo(name = "vigente") val vigente: Int = 0,
    @ColumnInfo(name = "cargoId") val cargoId: Int
)