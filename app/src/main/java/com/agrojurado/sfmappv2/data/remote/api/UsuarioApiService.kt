package com.agrojurado.sfmappv2.data.remote.api

import com.agrojurado.sfmappv2.data.remote.dto.login.LoginRequest
import com.agrojurado.sfmappv2.data.remote.dto.login.LoginResponse
import com.agrojurado.sfmappv2.data.remote.dto.usuario.UsuarioRequest
import com.agrojurado.sfmappv2.data.remote.dto.usuario.UsuarioResponse
import retrofit2.Response
import retrofit2.http.*

interface UsuarioApiService {

    @GET("api/usuario/read.php")
    suspend fun getUsuarios(): Response<List<UsuarioResponse>>

    @POST("api/usuario/create.php")
    suspend fun createUsuario(@Body usuario: UsuarioRequest): Response<UsuarioResponse>

    @PUT("api/usuario/update.php")
    suspend fun updateUsuario(@Body usuario: UsuarioRequest): Response<UsuarioResponse>

    @DELETE("api/usuario/delete.php")
    suspend fun deleteUsuario(@Query("id") id: Int): Response<UsuarioResponse>

    @GET("api/usuario/readOne.php")
    suspend fun getUsuarioByEmail(@Query("email") email: String): Response<UsuarioResponse>

    @POST("api/auth/login.php")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>
}