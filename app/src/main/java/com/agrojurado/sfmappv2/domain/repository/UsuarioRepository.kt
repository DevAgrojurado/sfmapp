package com.agrojurado.sfmappv2.domain.repository

import com.agrojurado.sfmappv2.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

interface UsuarioRepository {

    suspend fun insert(usuario: Usuario): Int

    suspend fun delete(usuario: Usuario): Int

    suspend fun updateKey(id: Int, clave: String): Int

    suspend fun getUser(email: String, clave: String): Usuario?

    suspend fun getUserById(id: Int): Usuario?

    suspend fun existsAccount(): Int

    suspend fun insertAccount(usuario: Usuario): Usuario?

    suspend fun crearUsuarioPredeterminado()

    fun getAllUsersUseCase(): Flow<List<Usuario>>

    fun list(dato: String): Flow<List<Usuario>>

}