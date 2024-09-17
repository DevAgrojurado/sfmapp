package com.agrojurado.sfmappv2.di

import android.content.Context
import androidx.room.Room
import com.agrojurado.sfmappv2.data.dao.UsuarioDao
import com.agrojurado.sfmappv2.data.database.AppDatabase
import com.agrojurado.sfmappv2.data.repository.UsuarioRepositoryImpl
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


}