package com.agrojurado.sfmappv2.data.repository

import com.agrojurado.sfmappv2.data.dao.UsuarioDao
import com.agrojurado.sfmappv2.data.mapper.UsuarioMapper
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UsuarioRepositoryImpl @Inject constructor(
    private val dao: UsuarioDao
): UsuarioRepository {
    override suspend fun grabar(usuario: Usuario): Int {
        return if(usuario.id == 0)
            dao.insertar(UsuarioMapper.toDatabase(usuario)).toInt()
        else
            dao.actualizar(UsuarioMapper.toDatabase(usuario))
    }

    override suspend fun eliminar(usuario: Usuario): Int {
        return dao.eliminar(UsuarioMapper.toDatabase(usuario))
    }

    override suspend fun actualizarClave(id: Int, clave: String): Int {
        return dao.actualizarClave(id, clave)
    }

    override suspend fun obtenerUsuario(email: String, clave: String): Usuario? {
        return dao.obtenerUsuario(email, clave)?.let {
            UsuarioMapper.toDomain(it)
        }
    }

    override suspend fun obtenerUsuarioPorId(id: Int): Usuario? {
        return dao.obtenerUsuarioPorId(id)?.let {
            UsuarioMapper.toDomain(it)
        }
    }

    override suspend fun existeCuenta(): Int {
        return dao.existeCuenta()
    }

    override suspend fun grabarCuenta(usuario: Usuario): Usuario? {
        return dao.grabarCuenta(UsuarioMapper.toDatabase(usuario))?.let {
            UsuarioMapper.toDomain(it)
        }
    }

    override fun listar(dato: String): Flow<List<Usuario>> {
        return dao.listarPorNombre(dato).map {
            it.map { usuarioEntity ->
                UsuarioMapper.toDomain(usuarioEntity)
            }
        }
    }
}