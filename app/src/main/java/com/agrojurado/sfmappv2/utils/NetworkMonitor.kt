// NetworkMonitor.kt
package com.agrojurado.sfmappv2.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.LiveData
import com.agrojurado.sfmappv2.data.remote.dto.common.utils.NetworkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMonitor @Inject constructor(@ApplicationContext private val context: Context) : LiveData<Boolean>() {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Red disponible")
            postValue(true)
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "Red perdida")
            postValue(false)
        }
    }

    private var isCallbackRegistered = false

    companion object {
        private const val TAG = "NetworkMonitor"
        @Volatile
        private var instance: NetworkMonitor? = null

        // Proporciona una forma de obtener la instancia singleton desde cualquier parte de la app
        fun getInstance(context: Context): NetworkMonitor {
            return instance ?: synchronized(this) {
                instance ?: NetworkMonitor(context.applicationContext).also { 
                    instance = it 
                    it.registerCallback()
                }
            }
        }
    }

    override fun onActive() {
        super.onActive()
        registerCallback()
    }

    override fun onInactive() {
        super.onInactive()
        // No desregistramos el callback aquí para mantener un solo registro
        // Esto evita el problema de TooManyRequestsException
        // unregisterCallback()
    }

    /**
     * Registra el callback de red de manera segura
     */
    fun registerCallback() {
        try {
            if (!isCallbackRegistered) {
                Log.d(TAG, "Registrando callback de red")
                val networkRequest = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
                isCallbackRegistered = true
                // Actualizamos el estado inicial
                postValue(NetworkManager.isNetworkAvailable(context))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al registrar callback de red: ${e.message}", e)
        }
    }

    /**
     * Desregistra el callback de red de manera segura
     */
    fun unregisterCallback() {
        try {
            if (isCallbackRegistered) {
                Log.d(TAG, "Desregistrando callback de red")
                connectivityManager.unregisterNetworkCallback(networkCallback)
                isCallbackRegistered = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al desregistrar callback de red: ${e.message}", e)
        }
    }

    /**
     * Obtiene el estado actual de la conexión
     */
    fun isNetworkAvailable(): Boolean {
        return NetworkManager.isNetworkAvailable(context)
    }
}