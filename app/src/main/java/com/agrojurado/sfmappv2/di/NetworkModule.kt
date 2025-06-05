// NetworkModule.kt
package com.agrojurado.sfmappv2.di

import android.content.Context
import com.agrojurado.sfmappv2.data.remote.api.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.GsonBuilder
import com.agrojurado.sfmappv2.data.remote.adapter.BooleanTypeAdapter
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val gson = GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(Boolean::class.java, BooleanTypeAdapter())
            .create()

        return Retrofit.Builder()
            .baseUrl("https://sfm.agrojurado.com/apisfmtest/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideAreaApiService(retrofit: Retrofit): AreaApiService {
        return retrofit.create(AreaApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideCargoApiService(retrofit: Retrofit): CargoApiService {
        return retrofit.create(CargoApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideFincaApiService(retrofit: Retrofit): FincaApiService {
        return retrofit.create(FincaApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideLoteApiService(retrofit: Retrofit): LoteApiService {
        return retrofit.create(LoteApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideOperarioApiService(retrofit: Retrofit): OperarioApiService {
        return retrofit.create(OperarioApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideEvaluacionApiService(retrofit: Retrofit): EvaluacionApiService {
        return retrofit.create(EvaluacionApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideEvaluacionGeneralApiService(retrofit: Retrofit): EvaluacionGeneralApiService {
        return retrofit.create(EvaluacionGeneralApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideUsuarioApiService(retrofit: Retrofit): UsuarioApiService {
        return retrofit.create(UsuarioApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
}