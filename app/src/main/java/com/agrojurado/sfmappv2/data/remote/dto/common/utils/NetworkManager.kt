package com.agrojurado.sfmappv2.data.remote.dto.common.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

object NetworkManager {

    private const val TAG = "Utils"

    enum class NetworkSpeed {
        FAST, MEDIUM, SLOW, NONE
    }

    // Método para verificar la conectividad de red VALIDADA
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // Método para clasificar la velocidad de la red
    fun getNetworkSpeed(context: Context): NetworkSpeed {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return NetworkSpeed.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkSpeed.NONE

        // Verificar si la red está validada
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            return NetworkSpeed.NONE
        }

        // Obtener velocidad estimada (en kbps)
        val downstreamBandwidth = capabilities.linkDownstreamBandwidthKbps
        Log.d(TAG, "Velocidad estimada: $downstreamBandwidth kbps")

        return when {
            // Wi-Fi o Ethernet: generalmente rápidas
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                if (downstreamBandwidth >= 5000) NetworkSpeed.FAST // > 5 Mbps
                else NetworkSpeed.MEDIUM
            }
            // Datos móviles: depende de la velocidad
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                when {
                    downstreamBandwidth >= 10000 -> NetworkSpeed.FAST // > 10 Mbps (5G/4G rápido)
                    downstreamBandwidth >= 1000 -> NetworkSpeed.MEDIUM // 1-10 Mbps (4G/3G)
                    else -> NetworkSpeed.SLOW // < 1 Mbps (3G lento/2G)
                }
            }
            else -> NetworkSpeed.SLOW // Otros casos, asumir lento por seguridad
        }
    }

    // Método para mostrar alertas
    fun showAlert(context: Context, message: String) {
        showOnMainThread(context, message)
    }

    // Método para manejar errores
    fun logError(tag: String, e: Exception, messagePrefix: String) {
        Log.e(tag, "$messagePrefix: ${e.message}")
    }

    // Método genérico para validar datos de entrada
    fun <T> validateInput(input: T?): Boolean {
        return input != null && input.toString().isNotBlank()
    }

    fun showToast(context: Context, message: String) {
        showOnMainThread(context, message)
    }

    // Método seguro para mostrar Toast en el hilo principal
    private fun showOnMainThread(context: Context, message: String) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}