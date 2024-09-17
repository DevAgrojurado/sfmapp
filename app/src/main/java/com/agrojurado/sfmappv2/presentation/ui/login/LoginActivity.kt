package com.agrojurado.sfmappv2.presentation.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.agrojurado.sfmappv2.databinding.ActivityLoginBinding
import com.agrojurado.sfmappv2.presentation.common.UiState
import com.agrojurado.sfmappv2.presentation.ui.crearcuenta.CrearCuentaActivity
import com.agrojurado.sfmappv2.presentation.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import pe.pcs.libpcs.UtilsCommon
import pe.pcs.libpcs.UtilsMessage
import pe.pcs.libpcs.UtilsSecurity

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

       binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListener()
        initObserver()

    }

    private fun initListener() {
        binding.tvCrearCuenta.setOnClickListener {
            UtilsCommon.hideKeyboard(this, it)
            viewModel.existeCuenta()
        }

        binding.btAcceder.setOnClickListener {
            UtilsCommon.hideKeyboard(this, it)

            if(binding.etEmail1.text.toString().trim().isEmpty() ||
                binding.etClave.text.toString().trim().isEmpty()){
                UtilsMessage.showAlertOk("ADVERTENCIA", "Todos los campos son obligatorios", this)
                return@setOnClickListener
            }

            viewModel.login(
                binding.etEmail1.text.toString().trim(),
                UtilsSecurity.createHashSha512(binding.etClave.text.toString().trim())
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

                    if(it.data > 0){
                        UtilsMessage.showAlertOk("ERROR", "Ya existe una cuenta", this)
                        return@observe
                    }

                    startActivity(Intent(this, CrearCuentaActivity::class.java))

                }
                null -> Unit
            }
        }

        viewModel.UiStateLogin.observe(this){
            when (it) {
                is UiState.Error -> {
                    binding.progressBar.isVisible = false
                    UtilsMessage.showAlertOk("ERROR", it.message, this)
                    viewModel.resetUiStateLogin()
                }
                UiState.Loading -> binding.progressBar.isVisible = true
                is UiState.Success -> {
                    binding.progressBar.isVisible = false

                    if(it.data == null) {
                        UtilsMessage.showAlertOk(
                            "ADVERTENCIA",
                            "El email y/o la clave no son correcto",
                            this
                        )
                        return@observe
                    }

                    MainActivity.mUsuario = it.data

                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                null -> Unit
            }
        }

    }

}