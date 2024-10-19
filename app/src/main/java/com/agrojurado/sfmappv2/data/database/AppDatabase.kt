package com.agrojurado.sfmappv2.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.agrojurado.sfmappv2.data.dao.AreaDao
import com.agrojurado.sfmappv2.data.dao.CargoDao
import com.agrojurado.sfmappv2.data.dao.EvaluacionPolinizacionDao
import com.agrojurado.sfmappv2.data.dao.FincaDao
import com.agrojurado.sfmappv2.data.dao.OperarioDao
import com.agrojurado.sfmappv2.data.dao.UsuarioDao
import com.agrojurado.sfmappv2.data.entity.AreaEntity
import com.agrojurado.sfmappv2.data.entity.AsistenciaEntity
import com.agrojurado.sfmappv2.data.entity.CargoEntity
import com.agrojurado.sfmappv2.data.entity.UsuarioEntity
import com.agrojurado.sfmappv2.data.entity.EvaluacionPolinizacionEntity
import com.agrojurado.sfmappv2.data.entity.FincaEntity
import com.agrojurado.sfmappv2.data.entity.LoteEntity
import com.agrojurado.sfmappv2.data.entity.OperarioEntity

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
}