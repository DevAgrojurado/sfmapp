// RetrofitClient.kt
package com.agrojurado.sfmappv2.data.remote.api

import com.agrojurado.sfmappv2.data.remote.adapter.BooleanTypeAdapter
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://sfm.agrojurado.com/apisfm/"

    private val gson = GsonBuilder()
        .serializeNulls()
        .registerTypeAdapter(Boolean::class.java, BooleanTypeAdapter())
        .create()

    // Configurar OkHttpClient con tiempos de espera extendidos
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient) // Usar el OkHttpClient personalizado
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val areaApiService: AreaApiService by lazy {
        retrofit.create(AreaApiService::class.java)
    }

    val cargoApiService: CargoApiService by lazy {
        retrofit.create(CargoApiService::class.java)
    }

    val fincaApiService: FincaApiService by lazy {
        retrofit.create(FincaApiService::class.java)
    }

    val loteApiService: LoteApiService by lazy {
        retrofit.create(LoteApiService::class.java)
    }

    val operarioApiService: OperarioApiService by lazy {
        retrofit.create(OperarioApiService::class.java)
    }

    val evaluacionApiService: EvaluacionApiService by lazy {
        retrofit.create(EvaluacionApiService::class.java)
    }

    val usuarioApiService: UsuarioApiService by lazy {
        retrofit.create(UsuarioApiService::class.java)
    }

    val evaluacionGeneralApiService: EvaluacionGeneralApiService by lazy {
        retrofit.create(EvaluacionGeneralApiService::class.java)
    }
}