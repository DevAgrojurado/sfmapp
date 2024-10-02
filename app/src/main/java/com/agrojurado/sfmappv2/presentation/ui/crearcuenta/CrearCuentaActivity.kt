package com.agrojurado.sfmappv2.presentation.ui.crearcuenta

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.agrojurado.sfmappv2.databinding.ActivityCrearCuentaBinding
import com.agrojurado.sfmappv2.domain.model.Cargo
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.presentation.common.UiState
import dagger.hilt.android.AndroidEntryPoint
import pe.pcs.libpcs.UtilsCommon
import pe.pcs.libpcs.UtilsMessage
import pe.pcs.libpcs.UtilsSecurity

@AndroidEntryPoint
class CrearCuentaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrearCuentaBinding
    private val viewModel: CrearCuentaViewModel by viewModels()
    private lateinit var cargoAdapter: ArrayAdapter<String>
    private var cargosList: List<Cargo> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearCuentaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        initListener()
        initObserver()
        setupCargoSpinner()
    }

    private fun setupUI() {
        binding.includeEditar.toolbar.title = "Crear Usuario"
        binding.includeEditar.toolbar.subtitle = "Ingresa los datos del usuario"
    }

    private fun initListener() {
        binding.includeEditar.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.fabGrabar.setOnClickListener {
            UtilsCommon.hideKeyboard(this, it)

            if (validateInputs()) {
                createUser()
            } else {
                UtilsMessage.showAlertOk("Advertencia", "Todos los campos son obligatorios", this)
            }
        }
    }

    private fun validateInputs(): Boolean {
        return binding.etContraseA.text.toString().trim().isNotEmpty() &&
                binding.etEmail.text.toString().trim().isNotEmpty() &&
                binding.etCedula.text.toString().trim().isNotEmpty() &&
                binding.etNombre.text.toString().trim().isNotEmpty() &&
                binding.etCodigo.text.toString().trim().isNotEmpty() &&
                binding.spinnerCargoU.selectedItemPosition != AdapterView.INVALID_POSITION
    }

    private fun createUser() {
        val selectedCargo = cargosList[binding.spinnerCargoU.selectedItemPosition]

        val nuevoUsuario = Usuario(
            id = 0,
            codigo = binding.etCodigo.text.toString().trim(),
            nombre = binding.etNombre.text.toString().trim(),
            cedula = binding.etCedula.text.toString().trim(),
            email = binding.etEmail.text.toString().trim(),
            clave = UtilsSecurity.createHashSha512(binding.etContraseA.text.toString().trim()),
            idCargo = selectedCargo.id,
            vigente = 1
        )

        viewModel.grabarCuenta(nuevoUsuario)
    }

    private fun initObserver() {
        viewModel.uiStateGrabar.observe(this) { state ->
            when (state) {
                is UiState.Error -> {
                    binding.progressBar.isVisible = false
                    UtilsMessage.showAlertOk("Error", state.message, this)
                    viewModel.resetUiStateGrabar()
                }
                UiState.Loading -> binding.progressBar.isVisible = true
                is UiState.Success -> {
                    binding.progressBar.isVisible = false
                    if (state.data != null) {
                        UtilsMessage.showToast(this, "Usuario creado con éxito")
                        finish()
                    }
                }
                null -> Unit
            }
        }

        viewModel.cargos.observe(this) { cargos ->
            cargosList = cargos
            cargoAdapter.clear()
            cargoAdapter.addAll(cargos.map { it.descripcion })
        }
    }

    private fun setupCargoSpinner() {
        cargoAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        cargoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCargoU.adapter = cargoAdapter
    }
}