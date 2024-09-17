package com.agrojurado.sfmappv2.presentation.ui.crearcuenta

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.agrojurado.sfmappv2.databinding.ActivityCrearCuentaBinding
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.presentation.common.UiState
import com.agrojurado.sfmappv2.presentation.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import pe.pcs.libpcs.UtilsCommon
import pe.pcs.libpcs.UtilsMessage
import pe.pcs.libpcs.UtilsSecurity

@AndroidEntryPoint
class CrearCuentaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrearCuentaBinding
    private val viewModel: CrearCuentaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityCrearCuentaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListener()
        initObserver()
    }

    private fun initListener(){

        binding.includeEditar.toolbar.title="Crear Usuario"
        binding.includeEditar.toolbar.subtitle="Ingresa lod datos del usuario"

        binding.includeEditar.toolbar.setNavigationOnClickListener{
            onBackPressedDispatcher.onBackPressed()
        }

        binding.fabGrabar.setOnClickListener{
            UtilsCommon.hideKeyboard(this, it)

            if(binding.etContraseA.text.toString().trim().isEmpty() ||
                binding.etEmail.text.toString().trim().isEmpty() ||
                binding.etCedula.text.toString().trim().isEmpty() ||
                binding.etNombre.text.toString().trim().isEmpty() ||
                binding.etCodigo.text.toString().trim().isEmpty()){
                UtilsMessage.showAlertOk("Advertencia", "Todos los campos son obligatorios", this)
                return@setOnClickListener
            }

            viewModel.grabarCuenta(
                Usuario().apply {
                    id = 0
                    codigo = binding.etCodigo.text.toString().trim()
                    nombre = binding.etNombre.text.toString().trim()
                    cedula = binding.etCedula.text.toString().trim()
                    email = binding.etEmail.text.toString().trim()
                    clave = UtilsSecurity.createHashSha512(binding.etContraseA.text.toString().trim())
                    vigente = 1
                }
            )

        }

    }

    private fun initObserver(){
        viewModel.uiStateGrabar.observe(this){
            when(it){
                is UiState.Error -> {
                    binding.progressBar.isVisible=false
                    UtilsMessage.showAlertOk("Error", it.message, this)
                    viewModel.resetUiStateGrabar()
                }
                UiState.Loading -> binding.progressBar.isVisible=true
                is UiState.Success -> {
                    binding.progressBar.isVisible=false

                    if(it.data == null) return@observe
                    //registrar usuario
                    MainActivity.mUsuario = it.data

                    UtilsMessage.showToast(this, "Usuario creado con exito")

                    startActivity(
                        Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    finish()
                }
                null -> Unit
            }
        }
    }
}