package com.agrojurado.sfmappv2.data.remote.api

import com.agrojurado.sfmappv2.data.remote.dto.cargo.CargoRequest
import com.agrojurado.sfmappv2.data.remote.dto.cargo.CargoResponse
import retrofit2.Response
import retrofit2.http.*

// ApiService.kt
interface CargoApiService {
    @GET("api/cargo/read.php")
    suspend fun getCargos(): Response<List<CargoResponse>>

    @POST("api/cargo/create.php")
    suspend fun createCargo(@Body cargo: CargoRequest): Response<CargoResponse>

    @PUT("api/cargo/update.php")
    suspend fun updateCargo(@Body cargo: CargoRequest): Response<CargoResponse>

    @DELETE("api/cargo/delete.php")
    suspend fun deleteCargo(@Query("id") id: Int): Response<CargoResponse>
}