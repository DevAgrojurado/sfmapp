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
import com.agrojurado.sfmappv2.domain.model.Operario
import com.agrojurado.sfmappv2.domain.model.Cargo
import com.agrojurado.sfmappv2.presentation.ui.base.BaseActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OperariosActivity : BaseActivity() {
    private lateinit var addsBtn: FloatingActionButton
    private lateinit var recv: RecyclerView
    private lateinit var operariosAdapter: OperariosAdapter
    private val viewModel: OperariosViewModel by viewModels()
    private var cargosList: List<Cargo> = listOf()

    override fun getLayoutResourceId(): Int = R.layout.activity_operarios
    override fun getActivityTitle(): String = "Operarios"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViews()
        setupRecyclerView()
        setupListeners()
        observeOperarios()
        observeCargos()
    }

    private fun initializeViews() {
        addsBtn = findViewById(R.id.addingBtnO)
        recv = findViewById(R.id.oRecycler)
    }

    private fun setupRecyclerView() {
        operariosAdapter = OperariosAdapter(this, ArrayList(), cargosList) { operario, action ->
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

    private fun observeOperarios() {
        viewModel.operarios.observe(this) { operarios ->
            operariosAdapter.updateOperarios(operarios)
        }
    }

    private fun observeCargos() {
        viewModel.cargos.observe(this) { cargos ->
            this.cargosList = cargos
            operariosAdapter.setCargos(cargos)
        }
    }

    private fun addInfo() {
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.add_operario, null)
        val etCodigo = v.findViewById<EditText>(R.id.et_codigo)
        val etNombre = v.findViewById<EditText>(R.id.et_nombre)
        val spinnerCargo = v.findViewById<Spinner>(R.id.spinnerCargo)

        val cargoNames = cargosList.map { it.descripcion }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cargoNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCargo.adapter = adapter

        val addDialog = AlertDialog.Builder(this)
        addDialog.setView(v)
        addDialog.setPositiveButton("Ok") { dialog, _ ->
            val codigo = etCodigo.text.toString()
            val nombre = etNombre.text.toString()

            if (cargosList.isNotEmpty()) {
                val cargoId = cargosList[spinnerCargo.selectedItemPosition].id
                if (codigo.isNotEmpty() && nombre.isNotEmpty()) {
                    lifecycleScope.launch {
                        viewModel.insertOperario(Operario(codigo = codigo, nombre = nombre, cargoId = cargoId))
                        Toast.makeText(this@OperariosActivity, "Operario agregado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Por favor ingresa todos los datos", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No hay cargos disponibles", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        addDialog.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        addDialog.create().show()
    }

    private fun updateOperario(operario: Operario) {
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.add_operario, null)
        val etCodigo = v.findViewById<EditText>(R.id.et_codigo)
        val etNombre = v.findViewById<EditText>(R.id.et_nombre)
        val spinnerCargo = v.findViewById<Spinner>(R.id.spinnerCargo)

        val cargoNames = cargosList.map { it.descripcion }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cargoNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCargo.adapter = adapter

        etCodigo.setText(operario.codigo)
        etNombre.setText(operario.nombre)

        val cargoPosition = cargosList.indexOfFirst { it.id == operario.cargoId }
        if (cargoPosition != -1) {
            spinnerCargo.setSelection(cargoPosition)
        }

        val updateDialog = AlertDialog.Builder(this)
        updateDialog.setView(v)
        updateDialog.setPositiveButton("Actualizar") { dialog, _ ->
            val codigo = etCodigo.text.toString()
            val nombre = etNombre.text.toString()
            val cargoId = cargosList[spinnerCargo.selectedItemPosition].id

            if (codigo.isNotEmpty() && nombre.isNotEmpty()) {
                lifecycleScope.launch {
                    viewModel.updateOperario(operario.copy(codigo = codigo, nombre = nombre, cargoId = cargoId))
                    Toast.makeText(this@OperariosActivity, "Operario actualizado", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor ingresa todos los datos", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        updateDialog.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        updateDialog.create().show()
    }

    private fun deleteOperario(operario: Operario) {
        lifecycleScope.launch {
            viewModel.deleteOperario(operario)
            Toast.makeText(this@OperariosActivity, "Operario eliminado", Toast.LENGTH_SHORT).show()
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
            operario.codigo.contains(query, ignoreCase = true) ||
                    operario.nombre.contains(query, ignoreCase = true) ||
                    cargoDescripcion.contains(query, ignoreCase = true)
        } ?: emptyList()
        operariosAdapter.updateOperarios(filteredList)
    }
}