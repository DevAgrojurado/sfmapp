package com.agrojurado.sfmappv2.data.remote.api

import com.agrojurado.sfmappv2.data.remote.dto.area.AreaRequest
import com.agrojurado.sfmappv2.data.remote.dto.area.AreaResponse
import retrofit2.Response
import retrofit2.http.*

// ApiService.kt
interface AreaApiService {
    @GET("api/area/read.php")
    suspend fun getAreas(): Response<List<AreaResponse>>

    @POST("api/area/create.php")
    suspend fun createArea(@Body area: AreaRequest): Response<AreaResponse>

    @PUT("api/area/update.php")
    suspend fun updateArea(@Body area: AreaRequest): Response<AreaResponse>

    @DELETE("api/area/delete.php")
    suspend fun deleteArea(@Query("id") id: Int): Response<AreaResponse>
}