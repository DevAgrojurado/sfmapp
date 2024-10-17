package com.agrojurado.sfmappv2.di

import android.content.Context
import androidx.room.Room
import com.agrojurado.sfmappv2.data.dao.*
import com.agrojurado.sfmappv2.data.database.AppDatabase
import com.agrojurado.sfmappv2.data.repository.*
import com.agrojurado.sfmappv2.domain.repository.*
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

    // Proveer la base de datos
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME

            // Solo purebas
        ).fallbackToDestructiveMigration()
            .build()
    }

    // Proveer DAOs
    @Singleton
    @Provides
    fun provideUsuarioDao(db: AppDatabase): UsuarioDao = db.usuarioDao()

    @Singleton
    @Provides
    fun provideCargoDao(db: AppDatabase): CargoDao = db.cargoDao()

    @Singleton
    @Provides
    fun provideOperarioDao(db: AppDatabase): OperarioDao = db.operarioDao()

    @Singleton
    @Provides
    fun provideAreaDao(db: AppDatabase): AreaDao = db.areaDao()

    @Singleton
    @Provides
    fun provideEvaluacionDao(db: AppDatabase): EvaluacionPolinizacionDao = db.evaluacionDao()

    // Proveer Repositorios
    @Singleton
    @Provides
    fun provideUsuarioRepository(
        dao: UsuarioDao,
        @ApplicationContext context: Context
    ): UsuarioRepository {
        return UsuarioRepositoryImpl(dao, context)
    }

    @Singleton
    @Provides
    fun provideCargoRepository(dao: CargoDao): CargoRepository {
        return CargoRepositoryImpl(dao)
    }

    @Singleton
    @Provides
    fun provideOperarioRepository(dao: OperarioDao): OperarioRepository {
        return OperarioRepositoryImpl(dao)
    }

    @Singleton
    @Provides
    fun provideAreaRepository(dao: AreaDao): AreaRepository {
        return AreaRepositoryImpl(dao)
    }

    @Singleton
    @Provides
    fun provideEvaluacionPolinizacionRepository(
        dao: EvaluacionPolinizacionDao
    ): EvaluacionPolinizacionRepository {
        return EvaluacionPolinizacionRepositoryImpl(dao)
    }
}
