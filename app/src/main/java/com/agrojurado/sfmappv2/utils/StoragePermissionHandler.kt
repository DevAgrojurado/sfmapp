package com.agrojurado.sfmappv2.utils

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class StoragePermissionHandler(private val fragment: Fragment) {

    companion object {
        const val STORAGE_PERMISSION_CODE = 101
    }

    /**
     * Verifica si los permisos de almacenamiento están concedidos según la versión de Android.
     */
    fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                fragment.requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Solicita los permisos adecuados según la versión de Android.
     */
    fun requestStoragePermission(onPermissionGranted: () -> Unit) {
        if (checkStoragePermission()) {
            onPermissionGranted()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ requiere permiso especial para gestionar archivos
                showPermissionDialog()
            } else {
                // Android 10 y versiones anteriores usan WRITE_EXTERNAL_STORAGE
                ActivityCompat.requestPermissions(
                    fragment.requireActivity(),
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    STORAGE_PERMISSION_CODE
                )
            }
        }
    }

    /**
     * Muestra un diálogo explicando por qué se necesita el permiso y guía al usuario.
     */
    private fun showPermissionDialog() {
        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Permiso Requerido")
            .setMessage("Para exportar archivos, la aplicación necesita acceso al almacenamiento. ¿Quieres activarlo?")
            .setPositiveButton("Aceptar") { _, _ ->
                openStorageSettings()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(
                    fragment.requireContext(),
                    "No se concedió el permiso. No se podrá exportar archivos.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .show()
    }

    /**
     * Abre la configuración de permisos para otorgar acceso al almacenamiento.
     */
    private fun openStorageSettings() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${fragment.requireContext().packageName}")
            }
            fragment.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            fragment.startActivity(intent)
        }
    }

    /**
     * Maneja el resultado de la solicitud de permisos.
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onPermissionGranted: () -> Unit
    ): Boolean {
        return when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onPermissionGranted()
                } else {
                    Toast.makeText(
                        fragment.requireContext(),
                        "Permiso de almacenamiento requerido para exportar archivos.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                true
            }
            else -> false
        }
    }
}
