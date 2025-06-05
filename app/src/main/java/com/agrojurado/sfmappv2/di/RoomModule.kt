package com.agrojurado.sfmappv2.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.agrojurado.sfmappv2.data.local.dao.*
import com.agrojurado.sfmappv2.data.local.database.AppDatabase
import com.agrojurado.sfmappv2.data.remote.api.AreaApiService
import com.agrojurado.sfmappv2.data.remote.api.CargoApiService
import com.agrojurado.sfmappv2.data.remote.api.EvaluacionApiService
import com.agrojurado.sfmappv2.data.remote.api.EvaluacionGeneralApiService
import com.agrojurado.sfmappv2.data.remote.api.FincaApiService
import com.agrojurado.sfmappv2.data.remote.api.LoteApiService
import com.agrojurado.sfmappv2.data.remote.api.OperarioApiService
import com.agrojurado.sfmappv2.data.remote.api.UsuarioApiService
import com.agrojurado.sfmappv2.data.repository.*
import com.agrojurado.sfmappv2.domain.repository.*
import com.agrojurado.sfmappv2.domain.security.RoleAccessControl
import com.agrojurado.sfmappv2.utils.NetworkMonitor
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
    fun provideEvaluacionGeneralDao(db: AppDatabase): EvaluacionGeneralDao = db.evaluacionGeneralDao()

    @Singleton
    @Provides
    fun provideLoteDao(db: AppDatabase): LoteDao = db.loteDao()

    @Singleton
    @Provides
    fun provideSyncQueueDao(db: AppDatabase): SyncQueueDao = db.syncQueueDao()

    // Proveer Repositorios
    @Singleton
    @Provides
    fun provideUsuarioRepository(
        dao: UsuarioDao,
        usuarioApiService: UsuarioApiService,
        @ApplicationContext context: Context
    ): UsuarioRepository {
        return UsuarioRepositoryImpl(dao, usuarioApiService, context)
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
    fun provideFinca(
        dao: FincaDao,
        fincaApiService: FincaApiService,
        @ApplicationContext context: Context
    ): FincaRepository {
        return FincaRepositoryImpl(dao, fincaApiService, context)
    }

    @Singleton
    @Provides
    fun provideOperarioRepository(
        dao: OperarioDao,
        operarioApiService: OperarioApiService,
        usuarioRepository: UsuarioRepository,
        roleAccessControl: RoleAccessControl,
        @ApplicationContext context: Context
    ): OperarioRepository {
        return OperarioRepositoryImpl(
            operarioDao = dao,
            operarioApiService = operarioApiService,
            usuarioRepository = usuarioRepository,
            roleAccessControl = roleAccessControl,
            context = context
        )
    }

    @Singleton
    @Provides
    fun provideAreaRepository(
        dao: AreaDao,
        areaApiService: AreaApiService,
        @ApplicationContext context: Context
    ): AreaRepository {
        return AreaRepositoryImpl(dao, areaApiService, context)
    }

    @Singleton
    @Provides
    fun provideEvaluacionPolinizacionRepository(
        dao: EvaluacionPolinizacionDao,
        @ApplicationContext context: Context
    ): EvaluacionPolinizacionRepository {
        return EvaluacionPolinizacionRepositoryImpl(dao, context)
    }

    @Provides
    @Singleton
    fun provideEvaluacionGeneralRepository(
        dao: EvaluacionGeneralDao,
        evaluacionPolinizacionRepository: EvaluacionPolinizacionRepository,
        @ApplicationContext context: Context
    ): EvaluacionGeneralRepository {
        return EvaluacionGeneralRepositoryImpl(dao, evaluacionPolinizacionRepository, context)
    }

    @Singleton
    @Provides
    fun provideLoteRepository(
        dao: LoteDao,
        loteApiService: LoteApiService,
        usuarioRepository: UsuarioRepository,
        roleAccessControl: RoleAccessControl,
        @ApplicationContext context: Context
    ): LoteRepository {
        return LoteRepositoryImpl(
            loteDao = dao,
            loteApiService = loteApiService,
            usuarioRepository = usuarioRepository,
            roleAccessControl = roleAccessControl,
            context = context
        )
    }

    @Singleton
    @Provides
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitor(context)
    }

    @Singleton
    @Provides
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}