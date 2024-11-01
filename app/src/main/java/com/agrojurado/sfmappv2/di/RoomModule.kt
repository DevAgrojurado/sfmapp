package com.agrojurado.sfmappv2.di

import android.content.Context
import androidx.room.Room
import com.agrojurado.sfmappv2.data.local.dao.*
import com.agrojurado.sfmappv2.data.local.database.AppDatabase
import com.agrojurado.sfmappv2.data.remote.api.AreaApiService
import com.agrojurado.sfmappv2.data.remote.api.CargoApiService
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
    fun provideFincaDao(db: AppDatabase): FincaDao = db.fincaDao()

    @Singleton
    @Provides
    fun provideOperarioDao(db: AppDatabase): OperarioDao = db.operarioDao()

    @Singleton
    @Provides
    fun provideAreaDao(db: AppDatabase): AreaDao = db.areaDao()

    @Singleton
    @Provides
    fun provideEvaluacionDao(db: AppDatabase): EvaluacionPolinizacionDao = db.evaluacionDao()

    @Singleton
    @Provides
    fun provideLoteDao(db: AppDatabase): LoteDao = db.loteDao()

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
    fun provideCargoRepository(
        dao: CargoDao,
        cargoApiService: CargoApiService,
        @ApplicationContext context: Context
    ): CargoRepository {
        return CargoRepositoryImpl(dao, cargoApiService, context)
    }

    @Singleton
    @Provides
    fun provideFinca(dao: FincaDao): FincaRepository {
        return FincaRepositoryImpl(dao)
    }

    @Singleton
    @Provides
    fun provideOperarioRepository(dao: OperarioDao): OperarioRepository {
        return OperarioRepositoryImpl(dao)
    }

    @Singleton
    @Provides
    fun provideAreaRepository(
        dao: AreaDao,
        areaApiService: AreaApiService,
        @ApplicationContext context: Context // Agregar context
    ): AreaRepository {
        return AreaRepositoryImpl(dao, areaApiService, context)
    }

    @Singleton
    @Provides
    fun provideEvaluacionPolinizacionRepository(
        dao: EvaluacionPolinizacionDao
    ): EvaluacionPolinizacionRepository {
        return EvaluacionPolinizacionRepositoryImpl(dao)
    }

    @Singleton
    @Provides
    fun provideLoteRepository(dao: LoteDao): LoteRepository {
        return LoteRepositoryImpl(dao)
    }
}
