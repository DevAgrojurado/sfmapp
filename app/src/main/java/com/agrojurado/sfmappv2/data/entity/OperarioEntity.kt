package com.agrojurado.sfmappv2.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "operario",
    //indices = [
        //Index(value = ["codigo"], unique = true),
        //Index(value = ["idcargo"]) // Adding this line to index the idcargo column
    //],
    //foreignKeys = [
        //ForeignKey(
            //entity = CargoEntity::class,
            //parentColumns = ["id"],
            //childColumns = ["idcargo"],
            //onDelete = ForeignKey.NO_ACTION
        //)
    //]
)
data class OperarioEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Int = 0,
    @ColumnInfo(name = "codigo") val codigo: String,
    @ColumnInfo(name = "nombre") val nombre: String,
    //@ColumnInfo(name = "idcargo") val idcargo: Int,
    @ColumnInfo(name = "vigente") val vigente: Int = 0
)
