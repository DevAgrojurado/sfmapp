package com.agrojurado.sfmappv2.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.agrojurado.sfmappv2.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao

interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usuario: UsuarioEntity): Long

    @Update
    suspend fun update(usuario: UsuarioEntity): Int

    @Delete
    suspend fun delete(usuario: UsuarioEntity): Int

    @Query("DELETE FROM usuario WHERE id = :id")
    suspend fun deleteById(id: Int): Int

    @Query("SELECT * FROM usuario Where nombre LIKE '%' || :nombre || '%'")
    fun listByName(nombre: String): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM usuario Where vigente=1 AND email=:email AND clave=:clave")
    suspend fun getUser(email: String, clave: String): UsuarioEntity?

    @Query("SELECT * FROM usuario")
    fun getAllUsuarios(): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM usuario Where id=:id")
    suspend fun getUserById(id: Int): UsuarioEntity?

    @Query("DELETE FROM usuario")
    suspend fun deleteAllUsuarios()

    @Query("SELECT ifnull(count(id), 0) FROM usuario")
    suspend fun existsAccount(): Int

    @Query("UPDATE usuario SET clave=:clave WHERE id=:id")
    suspend fun updateKey(id: Int, clave: String): Int

    @Query("SELECT COUNT(*) FROM usuario")
    suspend fun countUsuario(): Int

    @Query("SELECT * FROM usuario WHERE email = :email")
    fun getUserByEmail(email: String): Flow<UsuarioEntity?>

    @Transaction
    suspend fun setAccount(usuario: UsuarioEntity): UsuarioEntity?{
        return getUserById(
            insert(usuario).toInt()
        )
    }

    @Transaction
    suspend fun transaction(block: suspend () -> Unit) = block()
}