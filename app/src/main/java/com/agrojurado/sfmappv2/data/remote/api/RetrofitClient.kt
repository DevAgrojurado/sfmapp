// RetrofitClient.kt
package com.agrojurado.sfmappv2.data.remote.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://192.168.1.251/Projects/apiphp/" // Url del api

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
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
}
