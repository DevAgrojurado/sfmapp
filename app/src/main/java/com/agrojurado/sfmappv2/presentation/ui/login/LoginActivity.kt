package com.agrojurado.sfmappv2.presentation.ui.login

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.agrojurado.sfmappv2.databinding.ActivityLoginBinding
import com.agrojurado.sfmappv2.presentation.common.UiState
import com.agrojurado.sfmappv2.presentation.ui.crearcuenta.CreateUserActivity
import com.agrojurado.sfmappv2.presentation.ui.main.MainActivity
import com.agrojurado.sfmappv2.utils.LocationPermissionHandler
import com.agrojurado.sfmappv2.utils.NotificationPermissionHandler
import dagger.hilt.android.AndroidEntryPoint
import pe.pcs.libpcs.UtilsCommon
import pe.pcs.libpcs.UtilsMessage

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private var locationString: String? = null
    private lateinit var locationHandler: LocationPermissionHandler
    private val handler = Handler(Looper.getMainLooper())

    // Launcher para solicitar permiso de notificaciones
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permiso concedido, podemos mostrar notificaciones
        } else {
            // Permiso denegado, informar al usuario sobre la importancia de las notificaciones
            UtilsMessage.showAlertOk(
                "ADVERTENCIA",
                "Las notificaciones son necesarias para ver el progreso de sincronización",
                this
            )
        }
        
        // Después de procesar el permiso de notificaciones, solicitar ubicación
        handler.postDelayed({
            // Request location después de notificaciones
            locationHandler.requestLocation()
        }, 500)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize LocationPermissionHandler
        locationHandler = LocationPermissionHandler(
            context = this,
            activity = this,
            onLocationReceived = { location ->
                locationString = location
            },
            onPermissionDenied = {
                UtilsMessage.showAlertOk("ADVERTENCIA", "Permiso de ubicación denegado", this)
            },
            onGPSDisabled = {
                UtilsMessage.showAlertOk("ADVERTENCIA", "GPS es necesario para obtener la ubicación", this)
            }
        )

        // Verificar si la sesión ya está iniciada
        if (viewModel.isSessionStarted()) {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("LOCATION", locationString)
            startActivity(intent)
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListener()
        initObserver()

        // Primero solicitar permiso de notificaciones
        requestNotificationPermission()
    }
    
    private fun requestNotificationPermission() {
        if (!NotificationPermissionHandler.hasNotificationPermission(this)) {
            NotificationPermissionHandler(this).requestNotificationPermissionIfNeeded(this, notificationPermissionLauncher)
        } else {
            // Si ya tenemos el permiso de notificaciones, solicitar ubicación
            handler.postDelayed({
                locationHandler.requestLocation()
            }, 500)
        }
    }

    private fun initListener() {
        binding.btAcceder.setOnClickListener {
            UtilsCommon.hideKeyboard(this, it)

            if (binding.etEmail1.text.toString().trim().isEmpty() ||
                binding.etClave.text.toString().trim().isEmpty()
            ) {
                UtilsMessage.showAlertOk("ADVERTENCIA", "Todos los campos son obligatorios", this)
                return@setOnClickListener
            }

            viewModel.login(
                binding.etEmail1.text.toString().trim(),
                binding.etClave.text.toString().trim()
            )
        }
    }

    private fun initObserver() {
        viewModel.uiStateExisteCuenta.observe(this) {
            when (it) {
                is UiState.Error -> {
                    binding.progressBar.isVisible = false
                    UtilsMessage.showAlertOk("ERROR", it.message, this)
                    viewModel.resetUiStateExisteCuenta()
                }
                UiState.Loading -> binding.progressBar.isVisible = true
                is UiState.Success -> {
                    binding.progressBar.isVisible = false

                    if (it.data > 0) {
                        UtilsMessage.showAlertOk("ERROR", "Ya existe una cuenta", this)
                        return@observe
                    }

                    startActivity(Intent(this, CreateUserActivity::class.java))
                }
                null -> Unit
            }
        }

        viewModel.uiStateLogin.observe(this) {
            when (it) {
                is UiState.Error -> {
                    binding.progressBar.isVisible = false
                    UtilsMessage.showAlertOk("ERROR", it.message, this)
                    viewModel.resetUiStateLogin()
                }
                UiState.Loading -> binding.progressBar.isVisible = true
                is UiState.Success -> {
                    binding.progressBar.isVisible = false

                    if (it.data == null) {
                        UtilsMessage.showAlertOk(
                            "ADVERTENCIA",
                            "El email y/o la clave no son correctos",
                            this
                        )
                        return@observe
                    }

                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("LOCATION", locationString)
                    startActivity(intent)
                    finish()
                }
                null -> Unit
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationHandler.handleRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        locationHandler.handleActivityResult(requestCode)
    }
}