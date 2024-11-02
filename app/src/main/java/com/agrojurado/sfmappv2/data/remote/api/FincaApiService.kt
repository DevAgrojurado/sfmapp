package com.agrojurado.sfmappv2.data.remote.api

import com.agrojurado.sfmappv2.data.remote.dto.finca.FincaRequest
import com.agrojurado.sfmappv2.data.remote.dto.finca.FincaResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.*


interface FincaApiService {
    @GET("api/finca/read.php")
    suspend fun getFincas(): Response<List<FincaResponse>>

    @POST("api/finca/create.php")
    suspend fun createFinca(@Body finca: FincaRequest): Response<FincaResponse>

    @PUT("api/finca/update.php")
    suspend fun updateFinca(@Body finca: FincaRequest): Response<FincaResponse>

    @DELETE("api/finca/delete.php")
    suspend fun deleteFinca(@Query("id") id: Int): Response<FincaResponse>
}