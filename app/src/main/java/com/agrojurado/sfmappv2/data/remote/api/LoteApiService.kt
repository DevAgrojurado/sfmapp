package com.agrojurado.sfmappv2.data.remote.api

import com.agrojurado.sfmappv2.data.remote.dto.lote.LoteRequest
import com.agrojurado.sfmappv2.data.remote.dto.lote.LoteResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.*

interface LoteApiService {

    @GET("api/lote/read.php")
    suspend fun getLotes(): Response<List<LoteResponse>>

    @GET("api/lote/read.php")
    suspend fun getLotesByFinca(@Query("fincaId") fincaId: Int): Response<List<LoteResponse>>

    @POST("api/lote/create.php")
    suspend fun createLote(@Body lote: LoteRequest): Response<LoteResponse>

    @PUT("api/lote/update.php")
    suspend fun updateLote(@Body lote: LoteRequest): Response<LoteResponse>

    @DELETE("api/lote/delete.php")
    suspend fun deleteLote(@Query("id") id: Int): Response<LoteResponse>

}