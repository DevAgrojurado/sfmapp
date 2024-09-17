package com.agrojurado.sfmappv2.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.agrojurado.sfmappv2.data.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao

interface UsuarioDao {

    @Insert
    suspend fun insertar(usuario: UsuarioEntity): Long

    @Update
    suspend fun actualizar(usuario: UsuarioEntity): Int

    @Delete
    suspend fun eliminar(usuario: UsuarioEntity): Int

    @Query("SELECT * FROM usuario Where nombre LIKE '%' || :nombre || '%'")
    fun listarPorNombre(nombre: String): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM usuario Where vigente=1 AND email=:email AND clave=:clave")
    suspend fun obtenerUsuario(email: String, clave: String): UsuarioEntity?

    @Query("SELECT * FROM usuario Where id=:id")
    suspend fun obtenerUsuarioPorId(id: Int): UsuarioEntity?

    @Query("SELECT ifnull(count(id), 0) FROM usuario")
    suspend fun existeCuenta(): Int

    @Query("UPDATE usuario SET clave=:clave WHERE id=:id")
    suspend fun actualizarClave(id: Int, clave: String): Int

    @Transaction
    suspend fun grabarCuenta(usuario: UsuarioEntity): UsuarioEntity?{
        return obtenerUsuarioPorId(
            insertar(usuario).toInt()
        )
    }
}