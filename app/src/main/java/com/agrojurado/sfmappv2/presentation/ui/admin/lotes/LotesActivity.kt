package com.agrojurado.sfmappv2.presentation.ui.admin.lotes

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
import com.agrojurado.sfmappv2.domain.model.Finca
import com.agrojurado.sfmappv2.domain.model.Lote
import com.agrojurado.sfmappv2.presentation.ui.base.BaseActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LotesActivity : BaseActivity() {
    private lateinit var addsBtn: FloatingActionButton
    private lateinit var recv: RecyclerView
    private lateinit var lotesAdapter: LotesAdapter
    private lateinit var syncButton: FloatingActionButton
    private val viewModel: LotesViewModel by viewModels()
    private var fincasList: List<Finca> = listOf()

    override fun getLayoutResourceId(): Int = R.layout.activity_lotes
    override fun getActivityTitle(): String = "Lotes"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        syncButton = findViewById(R.id.syncButtonL)
        syncButton.setOnClickListener {
            viewModel.performFullSync()
        }

        initializeViews()
        setupRecyclerView()
        setupListeners()
        observeLotes()
        observeFincas()
        observeNetworkState()
        observeLoadingState()
        observeErrors()
    }

    private fun initializeViews() {
        addsBtn = findViewById(R.id.addingBtnL)
        recv = findViewById(R.id.lRecycler)
    }

    private fun setupRecyclerView() {
        lotesAdapter = LotesAdapter(this, ArrayList(), fincasList) { lote, action ->
            when (action) {
                "update" -> updateLote(lote)
                "delete" -> deleteLote(lote)
            }
        }
        recv.layoutManager = LinearLayoutManager(this)
        recv.adapter = lotesAdapter
    }

    private fun setupListeners() {
        addsBtn.setOnClickListener { addInfo() }
    }

    private fun observeNetworkState() {
        lifecycleScope.launch {
            viewModel.isOnline.collect { isOnline ->
                val message = if (isOnline) "Conectado" else "Modo sin conexión"
                Toast.makeText(this@LotesActivity, message, Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@LotesActivity, it, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun observeLotes() {
        lifecycleScope.launch {
            viewModel.lotes.collect { lotes ->
                lotesAdapter.updateLotes(lotes)
            }
        }
    }

    private fun observeFincas() {
        lifecycleScope.launch {
            viewModel.fincas.collect { fincas ->
                this@LotesActivity.fincasList = fincas
                lotesAdapter.setFincas(fincas)
            }
        }
    }

    private fun addInfo() {
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.add_lote, null)
        val etLote = v.findViewById<EditText>(R.id.et_lotes)
        val spinnerFinca = v.findViewById<Spinner>(R.id.spinnerFincaL)

        setupSpinner(spinnerFinca, fincasList.map { it.descripcion })

        val addDialog = AlertDialog.Builder(this)
        addDialog.setView(v)
        addDialog.setPositiveButton("Ok") { dialog, _ ->
            val descripcion = etLote.text.toString()

            if (fincasList.isNotEmpty()) {
                val idFinca = fincasList[spinnerFinca.selectedItemPosition].id
                if (descripcion.isNotEmpty()) {
                    lifecycleScope.launch {
                        try {
                            viewModel.insertLote(Lote(descripcion = descripcion, idFinca = idFinca))
                            Toast.makeText(this@LotesActivity, "Lote agregado con éxito", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@LotesActivity,
                                "Error al guardar: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Por favor ingresa todos los datos", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No hay fincas disponibles", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        addDialog.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        addDialog.show()
    }

    private fun updateLote(lote: Lote) {
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.add_lote, null)
        val etLote = v.findViewById<EditText>(R.id.et_lotes)
        val spinnerFinca = v.findViewById<Spinner>(R.id.spinnerFincaL)

        setupSpinner(spinnerFinca, fincasList.map { it.descripcion })

        etLote.setText(lote.descripcion)
        setSpinnerSelection(spinnerFinca, fincasList, lote.idFinca!!)

        val updateDialog = AlertDialog.Builder(this)
        updateDialog.setView(v)
        updateDialog.setPositiveButton("Actualizar") { dialog, _ ->
            val descripcion = etLote.text.toString()
            val idFinca = fincasList[spinnerFinca.selectedItemPosition].id

            if (descripcion.isNotEmpty()) {
                lifecycleScope.launch {
                    try {
                        viewModel.updateLote(lote.copy(descripcion = descripcion, idFinca = idFinca))
                        Toast.makeText(this@LotesActivity, "Lote actualizado", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@LotesActivity,
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
        val position = list.indexOfFirst { (it as? Finca)?.id == id }
        if (position != -1) {
            spinner.setSelection(position)
        }
    }

    private fun deleteLote(lote: Lote) {
        lifecycleScope.launch {
            try {
                viewModel.deleteLote(lote)
                Toast.makeText(this@LotesActivity, "Lote eliminado", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@LotesActivity,
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
                filterLotes(newText ?: "")
                return true
            }
        })
        return true
    }

    private fun filterLotes(query: String) {
        val filteredList = viewModel.lotes.value?.filter { lote ->
            val fincaDescripcion = fincasList.find { it.id == lote.idFinca }?.descripcion ?: ""
            lote.descripcion!!.contains(query, ignoreCase = true) ||
                    fincaDescripcion.contains(query, ignoreCase = true)
        } ?: emptyList()
        lotesAdapter.updateLotes(filteredList)
    }
}