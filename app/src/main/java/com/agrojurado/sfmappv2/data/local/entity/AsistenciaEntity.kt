package com.agrojurado.sfmappv2.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "asistencia",
    //indices = [Index(value = ["codigooperario"])],
    //foreignKeys = [
        //ForeignKey(
            //entity = OperarioEntity::class,
            //parentColumns = ["codigo"],
            //childColumns = ["codigooperario"],
            //onDelete = ForeignKey.NO_ACTION
        //)
    //]
)
data class AsistenciaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Int = 0,
   // @ColumnInfo(name = "codigooperario") val codigooperario: String,
    @ColumnInfo(name = "fecha") val fecha: String,
    @ColumnInfo(name = "hora") val hora: String,
    @ColumnInfo(name = "tipo") val tipo: String
)