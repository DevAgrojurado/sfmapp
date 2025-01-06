package com.agrojurado.sfmappv2.data.remote.api

import com.agrojurado.sfmappv2.data.remote.dto.evaluacion.EvaluacionRequest
import com.agrojurado.sfmappv2.data.remote.dto.evaluacion.EvaluacionResponse
import retrofit2.Response
import retrofit2.http.*

interface EvaluacionApiService {

    @GET("api/evaluacionpolinizacion/read.php")
    suspend fun getEvaluaciones(): Response<List<EvaluacionResponse>>

    @GET("api/evaluacionpolinizacion/read_one.php")
    suspend fun getEvaluacionById(@Query("id") id: Int): Response<EvaluacionResponse>

    @POST("api/evaluacionpolinizacion/create.php")
    suspend fun createEvaluacion(@Body evaluacion: EvaluacionRequest): Response<EvaluacionResponse>

    @PUT("api/evaluacionpolinizacion/update.php")
    suspend fun updateEvaluacion(
        @Query("id") id: Int,
        @Body evaluacion: EvaluacionRequest
    ): Response<EvaluacionResponse>

    @DELETE("api/evaluacionpolinizacion/delete.php")
    suspend fun deleteEvaluacion(@Query("id") id: Int): Response<EvaluacionResponse>

    @POST("api/evaluacionpolinizacion/sync.php")
    suspend fun syncEvaluaciones(@Body evaluaciones: List<EvaluacionRequest>): Response<List<EvaluacionResponse>>
}
