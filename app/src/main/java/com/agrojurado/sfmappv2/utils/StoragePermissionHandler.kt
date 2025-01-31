package com.agrojurado.sfmappv2.utils

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class StoragePermissionHandler(private val fragment: Fragment) {
    companion object {
        const val STORAGE_PERMISSION_CODE = 101
    }

    fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            fragment.requireContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestStoragePermission(onPermissionGranted: () -> Unit) {
        if (checkStoragePermission()) {
            onPermissionGranted()
        } else {
            ActivityCompat.requestPermissions(
                fragment.requireActivity(),
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        }
    }

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
                        "Permiso de almacenamiento requerido para exportar PDF",
                        Toast.LENGTH_LONG
                    ).show()
                }
                true
            }
            else -> false
        }
    }
}