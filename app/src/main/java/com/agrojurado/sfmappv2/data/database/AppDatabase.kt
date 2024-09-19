package com.agrojurado.sfmappv2.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.agrojurado.sfmappv2.data.dao.CargoDao
import com.agrojurado.sfmappv2.data.dao.UsuarioDao
import com.agrojurado.sfmappv2.data.entity.AsistenciaEntity
import com.agrojurado.sfmappv2.data.entity.CargoEntity
import com.agrojurado.sfmappv2.data.entity.UsuarioEntity
import com.agrojurado.sfmappv2.data.entity.EvaluacionPolinizacion
import com.agrojurado.sfmappv2.data.entity.LoteEntity
import com.agrojurado.sfmappv2.data.entity.OperarioEntity

@Database(
    entities = [
        UsuarioEntity::class,
        AsistenciaEntity::class,
        CargoEntity::class,
        EvaluacionPolinizacion::class,
        LoteEntity::class,
        OperarioEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun cargoDao(): CargoDao
}