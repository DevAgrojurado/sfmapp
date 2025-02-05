package com.agrojurado.sfmappv2.data.repository

import android.content.Context
import android.util.Log
import com.agrojurado.sfmappv2.data.local.dao.UsuarioDao
import com.agrojurado.sfmappv2.data.mapper.UsuarioMapper
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.domain.repository.UsuarioRepository
import com.agrojurado.sfmappv2.data.remote.api.UsuarioApiService
import com.agrojurado.sfmappv2.data.remote.dto.common.utils.Utils
import com.agrojurado.sfmappv2.data.remote.dto.login.LoginRequest
import com.agrojurado.sfmappv2.data.remote.dto.login.LoginResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.Response
import javax.inject.Inject

class UsuarioRepositoryImpl @Inject constructor(
    private val usuarioDao: UsuarioDao,
    private val usuarioApiService: UsuarioApiService,
    @ApplicationContext private val context: Context
) : UsuarioRepository {

    companion object {
        private const val TAG = "UsuarioRepository"
        private const val PREFS_NAME = "login_prefs"
        private const val KEY_EMAIL = "email_usuario"
    }

    private fun isNetworkAvailable(): Boolean = Utils.isNetworkAvailable(context)

    private fun showAlert(message: String) = Utils.showAlert(context, message)

    private fun logError(response: Response<*>, message: String) {
        val errorBody = response.errorBody()?.string() ?: "No error body"
        Log.e(TAG, "$message - Status: ${response.code()} - Error: $errorBody")
    }

    override suspend fun insert(usuario: Usuario): Int {
        if (!isNetworkAvailable()) {
            throw Exception("No hay conexión a Internet")
        }

        try {
            Log.d(TAG, "Intentando crear usuario: $usuario")
            val usuarioRequest = UsuarioMapper.toRequest(usuario)
            val response = usuarioApiService.createUsuario(usuarioRequest)

            if (response.isSuccessful && response.body() != null) {
                val usuarioResponse = response.body()!!
                val usuarioEntity = UsuarioMapper.toDatabase(UsuarioMapper.fromResponse(usuarioResponse))
                usuarioDao.insert(usuarioEntity)
                syncUsuarios()
                Log.d(TAG, "Usuario creado exitosamente con ID: ${usuarioResponse.id}")
                return usuarioResponse.id
            } else {
                logError(response, "Error al crear usuario")
                throw Exception("Error del servidor al crear usuario")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en creación de usuario", e)
            throw Exception("Error al crear usuario: ${e.message}")
        }
    }

    override suspend fun delete(usuario: Usuario): Int {
        return deleteUsuario(usuario)
    }

    override suspend fun getUserByEmailFromServer(email: String): Usuario? {
        if (!isNetworkAvailable()) return null

        return try {
            val response = usuarioApiService.getUsuarioByEmail(email)
            if (response.isSuccessful && response.body() != null) {
                val usuario = UsuarioMapper.fromResponse(response.body()!!)
                usuarioDao.insert(UsuarioMapper.toDatabase(usuario))
                usuario
            } else {
                Log.d(TAG, "Usuario no encontrado en servidor para email: $email")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener usuario por email", e)
            null
        }
    }

    override suspend fun getUser(email: String, clave: String): Usuario? {
        return try {
            if (!isNetworkAvailable()) {
                return usuarioDao.getUser(email, clave)?.let { UsuarioMapper.toDomain(it) }
            }

            val loginRequest = LoginRequest(email = email, clave = clave)
            val response = usuarioApiService.login(loginRequest)

            if (response.isSuccessful && response.body()?.usuario != null) {
                val usuario = UsuarioMapper.fromResponse(response.body()!!.usuario!!)
                usuarioDao.insert(UsuarioMapper.toDatabase(usuario))
                usuario
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener usuario", e)
            null
        }
    }

    override suspend fun updateKey(id: Int, clave: String): Int {
        if (!isNetworkAvailable()) {
            throw Exception("No hay conexión a Internet")
        }

        return try {
            // TODO: Implementar llamada al API para actualizar clave
            val result = usuarioDao.updateKey(id, clave)
            if (result > 0) {
                Log.d(TAG, "Clave actualizada localmente para usuario ID: $id")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar clave", e)
            throw Exception("Error al actualizar clave: ${e.message}")
        }
    }

    override suspend fun getUserById(id: Int): Usuario? {
        return try {
            if (!isNetworkAvailable()) {
                return usuarioDao.getUserById(id)?.let { UsuarioMapper.toDomain(it) }
            }

            // TODO: Implementar llamada al API
            usuarioDao.getUserById(id)?.let { UsuarioMapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener usuario por ID", e)
            null
        }
    }

    override suspend fun existsAccount(): Int {
        return try {
            usuarioDao.existsAccount()
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar cuenta", e)
            0
        }
    }

    override suspend fun insertAccount(usuario: Usuario): Usuario? {
        if (!isNetworkAvailable()) {
            throw Exception("No hay conexión a Internet")
        }

        try {
            val usuarioRequest = UsuarioMapper.toRequest(usuario)
            val response = usuarioApiService.createUsuario(usuarioRequest)

            if (response.isSuccessful && response.body() != null) {
                val nuevoUsuario = UsuarioMapper.fromResponse(response.body()!!)
                usuarioDao.insert(UsuarioMapper.toDatabase(nuevoUsuario))
                return nuevoUsuario
            } else {
                logError(response, "Error al crear cuenta de usuario")
                throw Exception("Error al crear cuenta de usuario")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al insertar cuenta", e)
            throw e
        }
    }

    override suspend fun deleteAllUsuarios() {
        try {
            usuarioDao.deleteAllUsuarios()
            Log.d(TAG, "Base de datos local limpiada exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al limpiar base de datos local", e)
            throw Exception("Error al limpiar base de datos local: ${e.message}")
        }
    }

    override fun list(dato: String): Flow<List<Usuario>> {
        return usuarioDao.listByName(dato).map { list ->
            list.map { UsuarioMapper.toDomain(it) }
        }
    }

    override suspend fun deleteUsuario(usuario: Usuario): Int {
        val userId = usuario.id
        if (userId == null || userId <= 0) {
            Log.e(TAG, "El ID del usuario no es válido o no está proporcionado. Detalles: $usuario")
            throw Exception("Error: ID de usuario no proporcionado o inválido.")
        }

        // Verificamos la conexión a Internet
        if (!isNetworkAvailable()) {
            Log.e(TAG, "No hay conexión a Internet. No se puede proceder con la eliminación.")
            throw Exception("No hay conexión a Internet")
        }

        try {
            Log.d(TAG, "Intentando eliminar usuario con ID: $userId")
            val response = usuarioApiService.deleteUsuario(userId)

            return when {
                response.isSuccessful -> {
                    usuarioDao.deleteById(userId)
                    Log.d(TAG, "Usuario eliminado exitosamente del servidor y la base de datos local - ID: $userId")
                    1 // Retornar 1 como indicador de éxito
                }
                response.code() == 404 -> {
                    // El usuario no fue encontrado en el servidor, eliminamos localmente
                    usuarioDao.deleteById(userId)
                    Log.d(TAG, "Usuario no encontrado en el servidor, eliminado solo localmente - ID: $userId")
                    1 // Retornar 1 como indicador de éxito
                }
                else -> {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    Log.e(TAG, "Error al eliminar usuario del servidor - Status: ${response.code()} - Error: $errorBody")
                    throw Exception("Error del servidor: $errorBody")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en la eliminación de usuario con ID: $userId", e)
            throw Exception("Error en la eliminación de usuario: ${e.message}")
        }
    }

    override suspend fun updateUsuario(usuario: Usuario): Int {
        if (!isNetworkAvailable()) {
            throw Exception("No hay conexión a Internet")
        }

        try {
            val usuarioRequest = UsuarioMapper.toRequest(usuario)
            val response = usuarioApiService.updateUsuario(usuario.id!!, usuarioRequest)

            return when {
                response.isSuccessful -> {
                    val usuarioResponse = response.body()
                    if (usuarioResponse != null) {
                        val usuarioEntity = UsuarioMapper.toDatabase(UsuarioMapper.fromResponse(usuarioResponse))
                        usuarioDao.update(usuarioEntity)
                        Log.d(TAG, "Usuario actualizado exitosamente con ID: ${usuario.id}")
                        1
                    } else {
                        Log.e(TAG, "Respuesta vacía al actualizar usuario")
                        0
                    }
                }
                else -> {
                    logError(response, "Error al actualizar usuario")
                    throw Exception("Error del servidor al actualizar usuario")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en actualización de usuario", e)
            throw Exception("Error al actualizar usuario: ${e.message}")
        }
    }


    override suspend fun getLoggedInUserEmail(): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_EMAIL, null)
    }

    override fun getUserByEmail(email: String): Flow<Usuario?> {
        return usuarioDao.getUserByEmail(email).map { entity ->
            entity?.let { UsuarioMapper.toDomain(it) }
        }
    }

    override fun getAllUsersUseCase(): Flow<List<Usuario>> {
        return usuarioDao.getAllUsuarios().map { list ->
            list.map { UsuarioMapper.toDomain(it) }
        }
    }

    override suspend fun syncUsuarios() {
        if (!isNetworkAvailable()) {
            showAlert("No hay conexión a Internet")
            return
        }

        try {
            val response = usuarioApiService.getUsuarios()

            when {
                response.isSuccessful -> {
                    val serverUsuarios = response.body()?.filterNotNull() ?: emptyList()

                    for (usuario in serverUsuarios) {
                        val usuarioEntity = UsuarioMapper.toDatabase(UsuarioMapper.fromResponse(usuario))

                        // Verifica si el usuario ya existe en la base de datos local
                        val existingUsuario = usuarioDao.getUserById(usuarioEntity.id)
                        if (existingUsuario != null) {
                            // Si el usuario existe, actualízalo
                            usuarioDao.update(usuarioEntity)
                            Log.d(TAG, "Usuario actualizado: ${usuarioEntity.id}")
                        } else {
                            // Si el usuario no existe, insértalo
                            usuarioDao.insert(usuarioEntity)
                            Log.d(TAG, "Usuario insertado: ${usuarioEntity.id}")
                        }
                    }
                    Log.d(TAG, "Sincronización completada exitosamente")
                }
                response.code() == 404 -> {
                    Log.d(TAG, "No se encontraron usuarios en el servidor")
                }
                else -> {
                    logError(response, "Error al sincronizar usuarios")
                    throw Exception("Error al obtener usuarios del servidor")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en sincronización", e)
            throw Exception("Error en sincronización: ${e.message}")
        }
    }

    override suspend fun fullSync(): Boolean {
        if (!isNetworkAvailable()) {
            showAlert("No hay conexión a Internet")
            return false
        }

        try {
            Log.d(TAG, "Conexión exitosa al servidor, iniciando sincronización...")

            // Obtenemos los usuarios desde el servidor
            val response = usuarioApiService.getUsuarios()

            when {
                response.isSuccessful -> {
                    val serverUsuarios = response.body()?.filterNotNull() ?: emptyList()
                    Log.d(TAG, "Usuarios obtenidos del servidor: ${serverUsuarios.size} usuarios")

                    // Obtenemos los usuarios locales
                    val localUsuarios = usuarioDao.getAllUsuarios().first()
                    Log.d(TAG, "Usuarios locales: ${localUsuarios.size} usuarios")

                    // Eliminamos los usuarios locales que ya no están en el servidor
                    for (localUser in localUsuarios) {
                        if (localUser.id !in serverUsuarios.map { it.id }) {
                            usuarioDao.deleteById(localUser.id)
                            Log.d(TAG, "Usuario eliminado localmente: ${localUser.id}")
                        }
                    }

                    // Insertamos o actualizamos los usuarios del servidor en la base de datos local
                    for (usuario in serverUsuarios) {
                        val usuarioEntity = UsuarioMapper.toDatabase(UsuarioMapper.fromResponse(usuario))

                        // Verifica si el usuario ya existe en la base de datos local
                        val existingUsuario = usuarioDao.getUserById(usuarioEntity.id)
                        if (existingUsuario != null) {
                            usuarioDao.update(usuarioEntity)
                            Log.d(TAG, "Usuario actualizado: ${usuarioEntity.id}")
                        } else {
                            usuarioDao.insert(usuarioEntity)
                            Log.d(TAG, "Usuario insertado: ${usuarioEntity.id}")
                        }
                    }

                    Log.d(TAG, "Sincronización completa exitosa")
                    return true
                }
                response.code() == 404 || response.body().isNullOrEmpty() -> {
                    Log.d(TAG, "No se encontraron usuarios en el servidor")
                    usuarioDao.deleteAllUsuarios()  // Si no hay usuarios en el servidor, eliminar todos los locales
                    return true
                }
                else -> {
                    logError(response, "Error en sincronización completa")
                    throw Exception("Error al obtener datos del servidor")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en sincronización completa", e)
            throw Exception("Error en sincronización completa: ${e.message}")
        }
    }


    override suspend fun login(email: String, clave: String): LoginResponse? {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "No hay conexión de red disponible")
            return null
        }

        return try {
            val loginRequest = LoginRequest(email = email, clave = clave)
            val response = usuarioApiService.login(loginRequest)

            if (response.isSuccessful) {
                response.body()?.also { loginResponse ->
                    loginResponse.usuario?.let { usuarioResponse ->
                        val usuarioEntity = UsuarioMapper.toDatabase(UsuarioMapper.fromResponse(usuarioResponse))
                        usuarioDao.insert(usuarioEntity)
                    }
                }
            } else {
                logError(response, "Error en login")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción durante login", e)
            null
        }
    }
}