package com.agrojurado.sfmappv2.di

import android.content.Context
import androidx.room.Room
import com.agrojurado.sfmappv2.data.dao.AreaDao
import com.agrojurado.sfmappv2.data.dao.CargoDao
import com.agrojurado.sfmappv2.data.dao.OperarioDao
import com.agrojurado.sfmappv2.data.dao.UsuarioDao
import com.agrojurado.sfmappv2.data.database.AppDatabase
import com.agrojurado.sfmappv2.data.repository.AreaRepositoryImpl
import com.agrojurado.sfmappv2.data.repository.CargoRepositoryImpl
import com.agrojurado.sfmappv2.data.repository.OperarioRepositoryImpl
import com.agrojurado.sfmappv2.data.repository.UsuarioRepositoryImpl
import com.agrojurado.sfmappv2.domain.repository.AreaRepository
import com.agrojurado.sfmappv2.domain.repository.CargoRepository
import com.agrojurado.sfmappv2.domain.repository.OperarioRepository
import com.agrojurado.sfmappv2.domain.repository.UsuarioRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    private const val DATABASE_NAME = "sfmdb"

    //**** Proveer la db **** //
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context) = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        DATABASE_NAME
    ).fallbackToDestructiveMigration()
        .build()

    //**** Proveer el DAO **** //
    @Singleton
    @Provides
    fun provideUsuarioDao(db: AppDatabase) = db.usuarioDao()

    //**** Proveer el Repository **** //
    @Singleton
    @Provides
    fun provideUsuarioRepository(dao: UsuarioDao) : UsuarioRepository {
        return UsuarioRepositoryImpl(dao)
    }

    // Proveer los DAOs
    @Singleton
    @Provides
    fun provideCargoDao(db: AppDatabase): CargoDao {
        return db.cargoDao()
    }

    // Proveer el Repositorio
    @Singleton
    @Provides
    fun provideCargoRepository(dao: CargoDao): CargoRepository {
        return CargoRepositoryImpl(dao)
    }

    // Proveer los DAOs
    @Singleton
    @Provides
    fun provideOperarioDao(db: AppDatabase): OperarioDao {
        return db.operarioDao()
    }

    // Proveer el Repositorio
    @Singleton
    @Provides
    fun provideOperarioRepository(dao: OperarioDao): OperarioRepository {
        return OperarioRepositoryImpl(dao)
    }

    // Proveer los DAOs
    @Singleton
    @Provides
    fun provideAreaDao(db: AppDatabase): AreaDao {
        return db.areaDao()
    }

    // Proveer el Repositorio
    @Singleton
    @Provides
    fun provideAreaRepository(dao: AreaDao): AreaRepository {
        return AreaRepositoryImpl(dao)
    }
}