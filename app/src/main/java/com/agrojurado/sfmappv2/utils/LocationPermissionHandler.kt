package com.agrojurado.sfmappv2.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

class LocationPermissionHandler(
    private val context: Context,
    private val activity: FragmentActivity,
    private val onLocationReceived: (String) -> Unit,
    private val onPermissionDenied: () -> Unit,
    private val onGPSDisabled: () -> Unit
) : LocationListener {

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
        const val GPS_REQUEST_CODE = 2
    }

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun requestLocation() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            checkGPSEnabled()
        }
    }

    private fun checkGPSEnabled() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGPSDisabledAlert()
        } else {
            startLocationUpdates()
        }
    }

    private fun showGPSDisabledAlert() {
        AlertDialog.Builder(context)
            .setMessage("GPS está desactivado. ¿Desea activarlo?")
            .setCancelable(false)
            .setPositiveButton("Sí") { _, _ ->
                activity.startActivityForResult(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                    GPS_REQUEST_CODE
                )
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
                onGPSDisabled.invoke()
            }
            .show()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 10f, this)
        }
    }

    override fun onLocationChanged(location: Location) {
        val locationString = "${location.latitude},${location.longitude}"
        onLocationReceived.invoke(locationString)
        locationManager.removeUpdates(this)
    }

    fun handleRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkGPSEnabled()
                } else {
                    onPermissionDenied.invoke()
                }
            }
        }
    }

    fun handleActivityResult(requestCode: Int) {
        if (requestCode == GPS_REQUEST_CODE) {
            checkGPSEnabled()
        }
    }
}