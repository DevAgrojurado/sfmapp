package com.agrojurado.sfmappv2.data.repository

import android.content.Context
import com.agrojurado.sfmappv2.data.dao.UsuarioDao
import com.agrojurado.sfmappv2.data.entity.UsuarioEntity
import com.agrojurado.sfmappv2.data.mapper.UsuarioMapper
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.domain.repository.CargoRepository
import com.agrojurado.sfmappv2.domain.repository.UsuarioRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.pcs.libpcs.UtilsSecurity
import javax.inject.Inject

class UsuarioRepositoryImpl @Inject constructor(
    private val dao: UsuarioDao,
    @ApplicationContext private val context: Context
): UsuarioRepository {
    override suspend fun insert(usuario: Usuario): Int {
        return if(usuario.id == 0)
            dao.insert(UsuarioMapper.toDatabase(usuario)).toInt()
        else
            dao.update(UsuarioMapper.toDatabase(usuario))
    }

    override suspend fun delete(usuario: Usuario): Int {
        return dao.delete(UsuarioMapper.toDatabase(usuario))
    }

    override suspend fun updateKey(id: Int, clave: String): Int {
        return dao.updateKey(id, clave)
    }

    override suspend fun getUser(email: String, clave: String): Usuario? {
        return dao.getUser(email, clave)?.let {
            UsuarioMapper.toDomain(it)
        }
    }

    override suspend fun getUserById(id: Int): Usuario? {
        return dao.getUserById(id)?.let {
            UsuarioMapper.toDomain(it)
        }
    }

    override suspend fun existsAccount(): Int {
        return dao.existsAccount()
    }

    override suspend fun insertAccount(usuario: Usuario): Usuario? {
        return dao.setAccount(UsuarioMapper.toDatabase(usuario))?.let {
            UsuarioMapper.toDomain(it)
        }
    }

    override fun list(dato: String): Flow<List<Usuario>> {
        return dao.listByName(dato).map {
            it.map { usuarioEntity ->
                UsuarioMapper.toDomain(usuarioEntity)
            }
        }
    }

    override suspend fun deleteUsuario(usuario: Usuario) {
        dao.delete(UsuarioMapper.toDatabase(usuario))
    }

    override suspend fun getLoggedInUserEmail(): String? {
        val sharedPreferences = context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("email_usuario", null)
    }

    override fun getUserByEmail(email: String): Flow<Usuario?> {
        return dao.getUserByEmail(email).map { entity ->
            entity?.let { UsuarioMapper.toDomain(it) }
        }
    }

    override fun getAllUsersUseCase(): Flow<List<Usuario>> {
        return dao.getAllUsuarios().map {
            it.map { usuarioEntity ->
                UsuarioMapper.toDomain(usuarioEntity)
            }
        }
    }
}