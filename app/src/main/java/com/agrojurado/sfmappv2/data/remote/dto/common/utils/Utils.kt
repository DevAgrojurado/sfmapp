package com.agrojurado.sfmappv2.data.remote.dto.common.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast

object Utils {

    private const val TAG = "Utils"

    // Método para verificar la conectividad de red
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                )
    }

    // Método para mostrar alertas
    fun showAlert(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Método para manejar errores
    fun logError(tag: String, e: Exception, messagePrefix: String) {
        Log.e(tag, "$messagePrefix: ${e.message}")
    }

    // Método genérico para validar datos de entrada
    fun <T> validateInput(input: T?): Boolean {
        return input != null && input.toString().isNotBlank()
    }
}
