// NetworkModule.kt
package com.agrojurado.sfmappv2.di

import android.content.Context
import com.agrojurado.sfmappv2.data.remote.api.AreaApiService
import com.agrojurado.sfmappv2.data.remote.api.CargoApiService
import com.agrojurado.sfmappv2.data.remote.api.EvaluacionApiService
import com.agrojurado.sfmappv2.data.remote.api.EvaluacionGeneralApiService
import com.agrojurado.sfmappv2.data.remote.api.FincaApiService
import com.agrojurado.sfmappv2.data.remote.api.LoteApiService
import com.agrojurado.sfmappv2.data.remote.api.OperarioApiService
import com.agrojurado.sfmappv2.data.remote.api.RetrofitClient
import com.agrojurado.sfmappv2.data.remote.api.UsuarioApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun provideAreaApiService(): AreaApiService {
        return RetrofitClient.areaApiService
    }

    @Singleton
    @Provides
    fun provideCargoApiService(): CargoApiService {
        return RetrofitClient.cargoApiService
    }

    @Singleton
    @Provides
    fun provideFincaApiService(): FincaApiService {
        return RetrofitClient.fincaApiService
    }

    @Singleton
    @Provides
    fun provideLoteApiService(): LoteApiService {
        return RetrofitClient.loteApiService
    }

    @Singleton
    @Provides
    fun provideOperarioApiService(): OperarioApiService {
        return RetrofitClient.operarioApiService
    }

    @Singleton
    @Provides
    fun provideEvaluacionApiService(): EvaluacionApiService {
        return RetrofitClient.evaluacionApiService
    }

    @Provides
    @Singleton
    fun provideEvaluacionGeneralApiService(): EvaluacionGeneralApiService {
        return RetrofitClient.evaluacionGeneralApiService
    }

    @Singleton
    @Provides
    fun provideUsuarioApiService(): UsuarioApiService {
        return RetrofitClient.usuarioApiService
    }


    // Agregar para proveer el Context
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

}
