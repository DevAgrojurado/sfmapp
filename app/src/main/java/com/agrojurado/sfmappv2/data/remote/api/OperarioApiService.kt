package com.agrojurado.sfmappv2.data.remote.api

import com.agrojurado.sfmappv2.data.remote.dto.operario.OperarioRequest
import com.agrojurado.sfmappv2.data.remote.dto.operario.OperarioResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.*

interface OperarioApiService {

    @GET("api/operario/read.php")
    suspend fun getOperarios(): Response<List<OperarioResponse>>

    @GET("api/operario/read.php")
    suspend fun getOperariosByFinca(@Query("fincaId") fincaId: Int): Response<List<OperarioResponse>>

    @POST("api/operario/create.php")
    suspend fun createOperario(@Body operario: OperarioRequest): Response<OperarioResponse>

    @PUT("api/operario/update.php")
    suspend fun updateOperario(@Body operario: OperarioRequest): Response<OperarioResponse>

    @DELETE("api/operario/delete.php")
    suspend fun deleteOperario(@Query("id") id: Int): Response<OperarioResponse>

}