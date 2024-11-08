package com.agrojurado.sfmappv2.presentation.ui.admin.operarios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.Area
import com.agrojurado.sfmappv2.domain.model.Cargo
import com.agrojurado.sfmappv2.domain.model.Finca
import com.agrojurado.sfmappv2.domain.model.Operario
import com.agrojurado.sfmappv2.presentation.ui.base.BaseActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OperariosActivity : BaseActivity() {
    private lateinit var addsBtn: FloatingActionButton
    private lateinit var recv: RecyclerView
    private lateinit var operariosAdapter: OperariosAdapter
    private lateinit var syncButton: FloatingActionButton
    private val viewModel: OperariosViewModel by viewModels()
    private var cargosList: List<Cargo> = listOf()
    private var areasList: List<Area> = listOf()
    private var fincasList: List<Finca> = listOf()

    override fun getLayoutResourceId(): Int = R.layout.activity_operarios
    override fun getActivityTitle(): String = "Operarios"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        syncButton = findViewById(R.id.syncButtonO)
        syncButton.setOnClickListener {
            viewModel.performFullSync()
        }

        initializeViews()
        setupRecyclerView()
        setupListeners()
        observeOperarios()
        observeCargos()
        observeAreas()
        observeFincas()
        observeNetworkState()
        observeLoadingState()
        observeErrors()
    }

    private fun initializeViews() {
        addsBtn = findViewById(R.id.addingBtnO)
        recv = findViewById(R.id.oRecycler)
    }

    private fun setupRecyclerView() {
        operariosAdapter = OperariosAdapter(this, ArrayList(), cargosList, areasList, fincasList) { operario, action ->
            when (action) {
                "update" -> updateOperario(operario)
                "delete" -> deleteOperario(operario)
            }
        }
        recv.layoutManager = LinearLayoutManager(this)
        recv.adapter = operariosAdapter
    }

    private fun setupListeners() {
        addsBtn.setOnClickListener { addInfo() }
    }

    private fun observeNetworkState() {
        lifecycleScope.launch {
            viewModel.isOnline.collect { isOnline ->
                val message = if (isOnline) "Conectado" else "Modo sin conexión"
                Toast.makeText(this@OperariosActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeLoadingState() {
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                // Aquí puedes mostrar/ocultar un indicador de carga
            }
        }
    }

    private fun observeErrors() {
        lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(this@OperariosActivity, it, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun observeOperarios() {
        lifecycleScope.launch {
            viewModel.operarios.collect { operarios ->
                operariosAdapter.updateOperarios(operarios)
            }
        }
    }

    private fun observeCargos() {
        lifecycleScope.launch {
            viewModel.cargos.collect { cargos ->
                this@OperariosActivity.cargosList = cargos
                operariosAdapter.setCargos(cargos)
            }
        }
    }

    private fun observeAreas() {
        lifecycleScope.launch {
            viewModel.areas.collect { areas ->
                this@OperariosActivity.areasList = areas
                operariosAdapter.setAreas(areas)
            }
        }
    }

    private fun observeFincas() {
        lifecycleScope.launch {
            viewModel.fincas.collect { fincas ->
                this@OperariosActivity.fincasList = fincas
                operariosAdapter.setFincas(fincas)
            }
        }
    }

    private fun addInfo() {
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.add_operario, null)
        val etCodigo = v.findViewById<EditText>(R.id.et_codigo)
        val etNombre = v.findViewById<EditText>(R.id.et_nombre)
        val spinnerCargo = v.findViewById<Spinner>(R.id.spinnerCargo)
        val spinnerArea = v.findViewById<Spinner>(R.id.spinnerArea)
        val spinnerFinca = v.findViewById<Spinner>(R.id.spinnerFinca)

        setupSpinner(spinnerCargo, cargosList.map { it.descripcion })
        setupSpinner(spinnerArea, areasList.map { it.descripcion })
        setupSpinner(spinnerFinca, fincasList.map { it.descripcion })

        val addDialog = AlertDialog.Builder(this)
        addDialog.setView(v)
        addDialog.setPositiveButton("Ok") { dialog, _ ->
            val codigo = etCodigo.text.toString()
            val nombre = etNombre.text.toString()

            if (cargosList.isNotEmpty() && areasList.isNotEmpty() && fincasList.isNotEmpty()) {
                val cargoId = cargosList[spinnerCargo.selectedItemPosition].id
                val areaId = areasList[spinnerArea.selectedItemPosition].id
                val fincaId = fincasList[spinnerFinca.selectedItemPosition].id
                if (codigo.isNotEmpty() && nombre.isNotEmpty()) {
                    lifecycleScope.launch {
                        try {
                            viewModel.insertOperario(Operario(codigo = codigo, nombre = nombre, cargoId = cargoId, areaId = areaId, fincaId = fincaId))
                            Toast.makeText(this@OperariosActivity, "Operario agregado con éxito", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@OperariosActivity,
                                "Error al guardar: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Por favor ingresa todos los datos", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No hay datos disponibles para todas las opciones", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        addDialog.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        addDialog.show()
    }

    private fun updateOperario(operario: Operario) {
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.add_operario, null)
        val etCodigo = v.findViewById<EditText>(R.id.et_codigo)
        val etNombre = v.findViewById<EditText>(R.id.et_nombre)
        val spinnerCargo = v.findViewById<Spinner>(R.id.spinnerCargo)
        val spinnerArea = v.findViewById<Spinner>(R.id.spinnerArea)
        val spinnerFinca = v.findViewById<Spinner>(R.id.spinnerFinca)

        setupSpinner(spinnerCargo, cargosList.map { it.descripcion })
        setupSpinner(spinnerArea, areasList.map { it.descripcion })
        setupSpinner(spinnerFinca, fincasList.map { it.descripcion })

        etCodigo.setText(operario.codigo)
        etNombre.setText(operario.nombre)

        setSpinnerSelection(spinnerCargo, cargosList, operario.cargoId)
        setSpinnerSelection(spinnerArea, areasList, operario.areaId)
        setSpinnerSelection(spinnerFinca, fincasList, operario.fincaId)

        val updateDialog = AlertDialog.Builder(this)
        updateDialog.setView(v)
        updateDialog.setPositiveButton("Actualizar") { dialog, _ ->
            val codigo = etCodigo.text.toString()
            val nombre = etNombre.text.toString()
            val cargoId = cargosList[spinnerCargo.selectedItemPosition].id
            val areaId = areasList[spinnerArea.selectedItemPosition].id
            val fincaId = fincasList[spinnerFinca.selectedItemPosition].id

            if (codigo.isNotEmpty() && nombre.isNotEmpty()) {
                lifecycleScope.launch {
                    try {
                        viewModel.updateOperario(operario.copy(codigo = codigo, nombre = nombre, cargoId = cargoId, areaId = areaId, fincaId = fincaId))
                        Toast.makeText(this@OperariosActivity, "Operario actualizado", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@OperariosActivity,
                            "Error al actualizar: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else {
                Toast.makeText(this, "Por favor ingresa todos los datos", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        updateDialog.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        updateDialog.show()
    }

    private fun setupSpinner(spinner: Spinner, items: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun <T> setSpinnerSelection(spinner: Spinner, list: List<T>, id: Int) {
        val position = list.indexOfFirst { (it as? Cargo)?.id == id || (it as? Area)?.id == id || (it as? Finca)?.id == id }
        if (position != -1) {
            spinner.setSelection(position)
        }
    }

    private fun deleteOperario(operario: Operario) {
        lifecycleScope.launch {
            try {
                viewModel.deleteOperario(operario)
                Toast.makeText(this@OperariosActivity, "Operario eliminado", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@OperariosActivity,
                    "Error al eliminar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterOperarios(newText ?: "")
                return true
            }
        })
        return true
    }

    private fun filterOperarios(query: String) {
        val filteredList = viewModel.operarios.value?.filter { operario ->
            val cargoDescripcion = cargosList.find { it.id == operario.cargoId }?.descripcion ?: ""
            val areaDescripcion = areasList.find { it.id == operario.areaId }?.descripcion ?: ""
            val fincaDescripcion = fincasList.find { it.id == operario.fincaId }?.descripcion ?: ""
            operario.codigo.contains(query, ignoreCase = true) ||
                    operario.nombre.contains(query, ignoreCase = true) ||
                    cargoDescripcion.contains(query, ignoreCase = true) ||
                    areaDescripcion.contains(query, ignoreCase = true) ||
                    fincaDescripcion.contains(query, ignoreCase = true)
        } ?: emptyList()
        operariosAdapter.updateOperarios(filteredList)
    }
}