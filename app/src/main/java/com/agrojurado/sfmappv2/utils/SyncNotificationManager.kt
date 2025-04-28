package com.agrojurado.sfmappv2.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.agrojurado.sfmappv2.R

/**
 * Gestor de notificaciones para el proceso de sincronización.
 * Muestra notificaciones con barra de progreso para informar al usuario sobre
 * el estado de la sincronización de evaluaciones.
 */
class SyncNotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "sync_channel"
        private const val SYNC_NOTIFICATION_ID = 1001
        private const val TAG = "SyncNotificationManager"
        
        // Singleton instance
        @Volatile
        private var instance: SyncNotificationManager? = null
        
        fun getInstance(context: Context): SyncNotificationManager {
            return instance ?: synchronized(this) {
                instance ?: SyncNotificationManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    private var builder: NotificationCompat.Builder? = null
    private var lastUpdateTime = 0L
    private val MIN_UPDATE_INTERVAL = 1500L // 1.5 segundos entre actualizaciones
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Crea el canal de notificaciones (requerido para Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sincronización",
                NotificationManager.IMPORTANCE_LOW // Cambiado a LOW para minimizar la intrusión
            ).apply {
                description = "Notificaciones del proceso de sincronización"
                setShowBadge(false) // No mostrar badge en el ícono
                enableVibration(false) // Desactivar vibración
                enableLights(false) // Desactivar luz LED
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Canal de notificaciones creado: $CHANNEL_ID")
        }
    }
    
    /**
     * Inicia una notificación de sincronización
     */
    fun startSyncNotification(title: String, message: String) {
        // Verificar permiso antes de mostrar
        if (!NotificationPermissionHandler.hasNotificationPermission(context)) {
            Log.e(TAG, "No se pudo mostrar la notificación: Permiso de notificaciones no concedido")
            return // No podemos mostrar la notificación sin permiso
        }
        
        Log.d(TAG, "SDK Version: ${Build.VERSION.SDK_INT}, Nombre: ${Build.VERSION.RELEASE}")
        
        builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_sync)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Cambiado a LOW para minimizar la intrusión
            .setOngoing(true)
            .setOnlyAlertOnce(true) // Solo alerta la primera vez
        
        try {
            notificationManager.notify(SYNC_NOTIFICATION_ID, builder!!.build())
            lastUpdateTime = System.currentTimeMillis()
            Log.d(TAG, "Notificación mostrada correctamente: $title - $message")
        } catch (e: SecurityException) {
            Log.e(TAG, "Error de seguridad al mostrar notificación: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar notificación: ${e.message}", e)
        }
    }
    
    /**
     * Actualiza el progreso de sincronización
     */
    fun updateSyncProgress(progress: Int, max: Int, message: String? = null) {
        if (!NotificationPermissionHandler.hasNotificationPermission(context)) {
            Log.e(TAG, "No se pudo actualizar progreso: Permiso de notificaciones no concedido")
            return
        }
        
        // Limitar la frecuencia de las actualizaciones
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime < MIN_UPDATE_INTERVAL) {
            return // Evitar actualizaciones demasiado frecuentes
        }
        
        builder?.let {
            it.setProgress(max, progress, false)
            message?.let { msg -> it.setContentText(msg) }
            
            try {
                notificationManager.notify(SYNC_NOTIFICATION_ID, it.build())
                lastUpdateTime = currentTime
                Log.d(TAG, "Progreso actualizado: $progress/$max - $message")
            } catch (e: SecurityException) {
                Log.e(TAG, "Error de seguridad al actualizar progreso: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar progreso: ${e.message}", e)
            }
        } ?: run {
            Log.e(TAG, "No se pudo actualizar progreso: Builder es null")
        }
    }
    
    /**
     * Actualiza solo el mensaje sin cambiar el progreso
     */
    fun updateSyncMessage(message: String) {
        if (!NotificationPermissionHandler.hasNotificationPermission(context)) {
            Log.e(TAG, "No se pudo actualizar mensaje: Permiso de notificaciones no concedido")
            return
        }
        
        // Limitar la frecuencia de las actualizaciones
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime < MIN_UPDATE_INTERVAL) {
            return // Evitar actualizaciones demasiado frecuentes
        }
        
        builder?.let {
            it.setContentText(message)
            
            try {
                notificationManager.notify(SYNC_NOTIFICATION_ID, it.build())
                lastUpdateTime = currentTime
                Log.d(TAG, "Mensaje actualizado: $message")
            } catch (e: SecurityException) {
                Log.e(TAG, "Error de seguridad al actualizar mensaje: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar mensaje: ${e.message}", e)
            }
        } ?: run {
            Log.e(TAG, "No se pudo actualizar mensaje: Builder es null")
        }
    }
    
    /**
     * Finaliza la notificación de sincronización con éxito
     */
    fun completeSyncNotification(message: String) {
        if (!NotificationPermissionHandler.hasNotificationPermission(context)) {
            Log.e(TAG, "No se pudo completar notificación: Permiso de notificaciones no concedido")
            return
        }
        
        builder?.let {
            it.setContentText(message)
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
            
            try {
                notificationManager.notify(SYNC_NOTIFICATION_ID, it.build())
                Log.d(TAG, "Notificación completada: $message")
            } catch (e: SecurityException) {
                Log.e(TAG, "Error de seguridad al completar notificación: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al completar notificación: ${e.message}", e)
            }
        } ?: run {
            Log.e(TAG, "No se pudo completar notificación: Builder es null")
        }
    }
    
    /**
     * Finaliza la notificación de sincronización con error
     */
    fun errorSyncNotification(message: String) {
        if (!NotificationPermissionHandler.hasNotificationPermission(context)) {
            Log.e(TAG, "No se pudo mostrar notificación de error: Permiso de notificaciones no concedido")
            return
        }
        
        builder?.let {
            it.setContentText(message)
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_error)
            
            try {
                notificationManager.notify(SYNC_NOTIFICATION_ID, it.build())
                Log.d(TAG, "Notificación de error mostrada: $message")
            } catch (e: SecurityException) {
                Log.e(TAG, "Error de seguridad al mostrar notificación de error: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al mostrar notificación de error: ${e.message}", e)
            }
        } ?: run {
            Log.e(TAG, "No se pudo mostrar notificación de error: Builder es null")
        }
    }
    
    /**
     * Cancela la notificación de sincronización
     */
    fun cancelSyncNotification() {
        // No es necesario verificar permisos para cancelar una notificación existente
        try {
            notificationManager.cancel(SYNC_NOTIFICATION_ID)
            Log.d(TAG, "Notificación cancelada")
        } catch (e: Exception) {
            Log.e(TAG, "Error al cancelar notificación: ${e.message}", e)
        }
    }
} 