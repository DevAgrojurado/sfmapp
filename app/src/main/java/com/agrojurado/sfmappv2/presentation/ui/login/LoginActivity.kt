package com.agrojurado.sfmappv2.presentation.ui.login

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import dagger.hilt.android.AndroidEntryPoint
import pe.pcs.libpcs.UtilsCommon
import pe.pcs.libpcs.UtilsMessage

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private var locationString: String? = null
    private lateinit var locationHandler: LocationPermissionHandler

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

        // Request location
        locationHandler.requestLocation()
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