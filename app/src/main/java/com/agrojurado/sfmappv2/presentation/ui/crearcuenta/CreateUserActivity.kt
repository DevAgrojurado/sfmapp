package com.agrojurado.sfmappv2.presentation.ui.crearcuenta

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.agrojurado.sfmappv2.data.sync.DataSyncManager
import com.agrojurado.sfmappv2.databinding.ActivityCrearCuentaBinding
import com.agrojurado.sfmappv2.domain.model.Area
import com.agrojurado.sfmappv2.domain.model.Cargo
import com.agrojurado.sfmappv2.domain.model.Finca
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.domain.model.UserRoles
import com.agrojurado.sfmappv2.presentation.common.UiState
import dagger.hilt.android.AndroidEntryPoint
import pe.pcs.libpcs.UtilsCommon
import pe.pcs.libpcs.UtilsMessage
import javax.inject.Inject

@AndroidEntryPoint
class CreateUserActivity : AppCompatActivity() {

    @Inject
    lateinit var dataSyncManager: DataSyncManager
    private lateinit var binding: ActivityCrearCuentaBinding
    private val viewModel: CreateUserViewModel by viewModels()

    private lateinit var cargoAdapter: ArrayAdapter<String>
    private lateinit var areaAdapter: ArrayAdapter<String>
    private lateinit var fincaAdapter: ArrayAdapter<String>
    private lateinit var rolAdapter: ArrayAdapter<String>

    private var cargosList: List<Cargo> = listOf()
    private var areasList: List<Area> = listOf()
    private var fincasList: List<Finca> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearCuentaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val progressBar: ProgressBar = binding.progressBar

        setupUI()
        setupListeners()
        setupObservers()
        setupCargoSpinner()
        setupAreaSpinner()
        setupFincaSpinner()
        setupRoleSpinner()
        initializeSync(progressBar)
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
        viewModel.areas.observe(this, ::updateAreasList)
        viewModel.fincas.observe(this, ::updateFincasList)
    }

    private fun validateInputs(): Boolean {
        return binding.run {
            etContraseA.text.toString().isNotBlank() &&
                    etEmail.text.toString().isNotBlank() &&
                    etCedula.text.toString().isNotBlank() &&
                    etNombre.text.toString().isNotBlank() &&
                    etCodigo.text.toString().isNotBlank() &&
                    spinnerRol.selectedItem != null
        }
    }

    private fun createUser() {
        val selectedCargoPosition = binding.spinnerCargoU.selectedItemPosition
        val selectedAreaPosition = binding.spinnerAreaU.selectedItemPosition
        val selectedFincaPosition = binding.spinnerFincaU.selectedItemPosition
        val selectedRolPosition = binding.spinnerRol.selectedItemPosition

        val selectedCargo = if (selectedCargoPosition >= 0 && selectedCargoPosition < cargosList.size) {
            cargosList[selectedCargoPosition]
        } else null

        val selectedArea = if (selectedAreaPosition >= 0 && selectedAreaPosition < areasList.size) {
            areasList[selectedAreaPosition]
        } else null

        val selectedFinca = if (selectedFincaPosition >= 0 && selectedFincaPosition < fincasList.size) {
            fincasList[selectedFincaPosition]
        } else null

        val selectedRol = if (selectedRolPosition >= 0 && selectedRolPosition < UserRoles.entries.size) {
            UserRoles.entries[selectedRolPosition].name
        } else UserRoles.EVALUADOR.name

        val nuevoUsuario = Usuario(
            codigo = binding.etCodigo.text.toString().trim(),
            nombre = binding.etNombre.text.toString().trim(),
            cedula = binding.etCedula.text.toString().trim(),
            email = binding.etEmail.text.toString().trim(),
            clave = binding.etContraseA.text.toString().trim(),
            idCargo = selectedCargo?.id,
            idArea = selectedArea?.id,
            idFinca = selectedFinca?.id,
            rol = selectedRol,
            vigente = 1
        )

        viewModel.grabarCuenta(nuevoUsuario)
    }

    private fun setupRoleSpinner() {
        rolAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            UserRoles.entries.map { it.name }
        )
        rolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRol.adapter = rolAdapter
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

    private fun updateAreasList(areas: List<Area>) {
        areasList = areas
        areaAdapter.clear()
        areaAdapter.addAll(areas.map { it.descripcion })
    }

    private fun updateFincasList(fincas: List<Finca>) {
        fincasList = fincas
        fincaAdapter.clear()
        fincaAdapter.addAll(fincas.map { it.descripcion })
    }

    private fun setupCargoSpinner() {
        cargoAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        cargoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCargoU.adapter = cargoAdapter
    }

    private fun setupAreaSpinner() {
        areaAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        areaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAreaU.adapter = areaAdapter
    }

    private fun setupFincaSpinner() {
        fincaAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        fincaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFincaU.adapter = fincaAdapter
    }


    private fun showValidationError() {
        UtilsMessage.showAlertOk(
            "Advertencia",
            "Complete los campos obligatorios",
            this
        )
    }

    private fun initializeSync(progressBar: ProgressBar) {
        dataSyncManager.syncAllData(progressBar) {
            runOnUiThread {
                Toast.makeText(this, "Sincronización completada", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
