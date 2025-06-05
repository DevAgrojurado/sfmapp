package com.agrojurado.sfmappv2.data.remote.api

import com.agrojurado.sfmappv2.data.remote.dto.evaluaciongeneral.EvaluacionGeneralRequest
import com.agrojurado.sfmappv2.data.remote.dto.evaluaciongeneral.EvaluacionGeneralResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface EvaluacionGeneralApiService {

    @GET("api/evaluaciongeneral/read.php")
    suspend fun getEvaluacionesGenerales(): Response<List<EvaluacionGeneralResponse>>

    @GET("api/evaluaciongeneral/read_one.php")
    suspend fun getEvaluacionGeneralById(@Query("id") id: Int): Response<EvaluacionGeneralResponse>

    @POST("api/evaluaciongeneral/create.php")
    suspend fun createEvaluacionGeneral(@Body evaluacion: EvaluacionGeneralRequest): Response<EvaluacionGeneralResponse>

    @PUT("api/evaluaciongeneral/update.php")
    suspend fun updateEvaluacionGeneral(
        @Query("id") id: Int,
        @Body evaluacion: EvaluacionGeneralRequest
    ): Response<EvaluacionGeneralResponse>

    @DELETE("api/evaluaciongeneral/delete.php")
    suspend fun deleteEvaluacionGeneral(@Query("id") id: Int): Response<EvaluacionGeneralResponse>

    @POST("api/evaluaciongeneral/sync.php")
    suspend fun syncEvaluacionesGenerales(
        @Body request: Map<String, List<EvaluacionGeneralRequest>>
    ): Response<List<EvaluacionGeneralResponse>>

    @Multipart
    @POST("api/evaluaciongeneral/upload_photo.php")
    suspend fun uploadPhoto(
        @Query("evaluacionId") evaluacionId: Int,
        @Part photo: MultipartBody.Part
    ): Response<Map<String, String>> // Server returns {"url": "...", "message": "..."}

    @GET
    suspend fun downloadImage(@Url url: String): Response<okhttp3.ResponseBody>

    @Multipart
    @POST("api/evaluaciongeneral/upload_signature.php")
    suspend fun uploadSignature(
        @Query("evaluacionId") evaluacionId: Int,
        @Part signature: MultipartBody.Part
    ): Response<Map<String, String>> // Server returns {"url": "...", "message": "..."}

}