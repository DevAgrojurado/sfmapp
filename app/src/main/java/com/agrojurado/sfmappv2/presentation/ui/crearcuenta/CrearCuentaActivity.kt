package com.agrojurado.sfmappv2.presentation.ui.crearcuenta

import android.os.Bundle
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
        setupListeners()
        setupObservers()
        setupCargoSpinner()
    }

    private fun setupUI() {
        binding.includeEditar.toolbar.apply {
            title = "Crear Usuario"
            subtitle = "Ingresa los datos del usuario"
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
    }

    private fun setupListeners() {
        binding.fabGrabar.setOnClickListener {
            UtilsCommon.hideKeyboard(this, it)
            if (validateInputs()) createUser() else showValidationError()
        }
    }

    private fun setupObservers() {
        viewModel.uiStateGrabar.observe(this, ::handleUiState)
        viewModel.cargos.observe(this, ::updateCargosList)
    }

    private fun validateInputs(): Boolean {
        return binding.run {
            etContraseA.text.toString().isNotBlank() &&
                    etEmail.text.toString().isNotBlank() &&
                    etCedula.text.toString().isNotBlank() &&
                    etNombre.text.toString().isNotBlank() &&
                    etCodigo.text.toString().isNotBlank()
        }
    }

    private fun createUser() {
        val selectedCargoPosition = binding.spinnerCargoU.selectedItemPosition
        val selectedCargo = if (selectedCargoPosition >= 0 && selectedCargoPosition < cargosList.size) {
            cargosList[selectedCargoPosition]
        } else null

        val nuevoUsuario = Usuario(
            codigo = binding.etCodigo.text.toString().trim(),
            nombre = binding.etNombre.text.toString().trim(),
            cedula = binding.etCedula.text.toString().trim(),
            email = binding.etEmail.text.toString().trim(),
            clave = UtilsSecurity.createHashSha512(binding.etContraseA.text.toString().trim()),
            idCargo = selectedCargo?.id,
            vigente = 1
        )

        viewModel.grabarCuenta(nuevoUsuario)
    }

    private fun handleUiState(state: UiState<Usuario?>?) {
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

    private fun updateCargosList(cargos: List<Cargo>) {
        cargosList = cargos
        cargoAdapter.clear()
        cargoAdapter.addAll(cargos.map { it.descripcion })
    }

    private fun setupCargoSpinner() {
        cargoAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        cargoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCargoU.adapter = cargoAdapter
    }

    private fun showValidationError() {
        UtilsMessage.showAlertOk(
            "Advertencia",
            "Todos los campos son obligatorios excepto el cargo",
            this
        )
    }
}