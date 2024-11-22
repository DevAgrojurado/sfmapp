package com.agrojurado.sfmappv2.data.remote.api

import com.agrojurado.sfmappv2.data.remote.dto.evaluacion.EvaluacionRequest
import com.agrojurado.sfmappv2.data.remote.dto.evaluacion.EvaluacionResponse
import retrofit2.Response
import retrofit2.http.*

interface EvaluacionApiService {

    @GET("api/evaluacionpolinizacion/read.php")
    suspend fun getEvaluaciones(): Response<List<EvaluacionResponse>>


    @POST("api/evaluacionpolinizacion/create.php")
    suspend fun createEvaluacion(@Body evaluacion: EvaluacionRequest): Response<EvaluacionResponse>


    @PUT("api/evaluacionpolinizacion/update.php")
    suspend fun updateEvaluacion(@Body evaluacion: EvaluacionRequest): Response<EvaluacionResponse>


    @DELETE("api/evaluacionpolinizacion/delete.php")
    suspend fun deleteEvaluacion(@Query("id") id: Int): Response<EvaluacionResponse>


}