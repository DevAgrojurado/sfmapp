package com.agrojurado.sfmappv2.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Manejador de permisos de notificación para Android 13 (API 33) y superior.
 * En versiones anteriores, no es necesario solicitar el permiso explícitamente.
 */
class NotificationPermissionHandler(private val context: Context) {
    
    companion object {
        /**
         * Verifica si el permiso de notificación está concedido.
         * @param context Contexto de la aplicación
         * @return true si el permiso está concedido o no es necesario (Android < 13), false en caso contrario
         */
        fun hasNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                // En versiones anteriores a Android 13, no es necesario solicitar el permiso
                true
            }
        }
        
        /**
         * Crea y configura el launcher para la solicitud de permisos en una actividad
         */
        fun setupPermissionLauncher(activity: AppCompatActivity, onResult: (Boolean) -> Unit): ActivityResultLauncher<String> {
            return activity.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                onResult(isGranted)
            }
        }
        
        /**
         * Crea y configura el launcher para la solicitud de permisos en un fragmento
         */
        fun setupPermissionLauncher(fragment: Fragment, onResult: (Boolean) -> Unit): ActivityResultLauncher<String> {
            return fragment.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                onResult(isGranted)
            }
        }
    }
    
    /**
     * Solicita el permiso de notificación si es necesario.
     * @param activity Actividad desde la que se solicita el permiso
     * @param launcher Launcher configurado para procesar el resultado
     */
    fun requestNotificationPermissionIfNeeded(activity: Activity, launcher: ActivityResultLauncher<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
} 