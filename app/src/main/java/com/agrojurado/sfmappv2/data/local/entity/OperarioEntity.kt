package com.agrojurado.sfmappv2.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "operario",
    foreignKeys = [
        ForeignKey(
        entity = CargoEntity::class,
        parentColumns = ["id"],
        childColumns = ["cargoId"],
        onDelete = ForeignKey.NO_ACTION
    ),
    ForeignKey(
        entity = AreaEntity::class,
        parentColumns = ["id"],
        childColumns = ["areaId"],
        onDelete = ForeignKey.NO_ACTION
    ),
    ForeignKey(
        entity = FincaEntity::class,
        parentColumns = ["id"],
        childColumns = ["fincaId"],
        onDelete = ForeignKey.NO_ACTION
    ),
],
    indices = [
        Index(value = ["cargoId"]),
        Index(value = ["areaId"]),
        Index(value = ["fincaId"])
    ]
)
data class OperarioEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Int = 0,
    @ColumnInfo(name = "codigo") var codigo: String,
    @ColumnInfo(name = "nombre") var nombre: String,
    @ColumnInfo(name = "cargoId") val cargoId: Int,
    @ColumnInfo(name = "areaId") val areaId: Int,
    @ColumnInfo(name = "fincaId") val fincaId: Int,
    @ColumnInfo(name = "isSynced") var isSynced: Boolean = false

)