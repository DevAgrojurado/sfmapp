package com.agrojurado.sfmappv2.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.agrojurado.sfmappv2.data.local.dao.AreaDao
import com.agrojurado.sfmappv2.data.local.dao.CargoDao
import com.agrojurado.sfmappv2.data.local.dao.EvaluacionPolinizacionDao
import com.agrojurado.sfmappv2.data.local.dao.FincaDao
import com.agrojurado.sfmappv2.data.local.dao.LoteDao
import com.agrojurado.sfmappv2.data.local.dao.OperarioDao
import com.agrojurado.sfmappv2.data.local.dao.UsuarioDao
import com.agrojurado.sfmappv2.data.local.entity.AreaEntity
import com.agrojurado.sfmappv2.data.local.entity.AsistenciaEntity
import com.agrojurado.sfmappv2.data.local.entity.CargoEntity
import com.agrojurado.sfmappv2.data.local.entity.UsuarioEntity
import com.agrojurado.sfmappv2.data.local.entity.EvaluacionPolinizacionEntity
import com.agrojurado.sfmappv2.data.local.entity.FincaEntity
import com.agrojurado.sfmappv2.data.local.entity.LoteEntity
import com.agrojurado.sfmappv2.data.local.entity.OperarioEntity

@Database(
    entities = [
        UsuarioEntity::class,
        AsistenciaEntity::class,
        CargoEntity::class,
        EvaluacionPolinizacionEntity::class,
        LoteEntity::class,
        OperarioEntity::class,
        AreaEntity::class,
        FincaEntity::class


    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun cargoDao(): CargoDao
    abstract fun operarioDao(): OperarioDao
    abstract fun areaDao(): AreaDao
    abstract fun evaluacionDao(): EvaluacionPolinizacionDao
    abstract fun fincaDao(): FincaDao
    abstract fun loteDao(): LoteDao
}