package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.listaevaluacion

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.presentation.ui.base.BaseActivity
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.EvaluacionActivity
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.EvaluacionViewModel
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.operarioevaluacion.OperarioEvaluacionActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ListaEvaluacionActivity : BaseActivity() {
    private lateinit var addsBtn: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var semanaAdapter: ListaEvaluacionAdapter
    private lateinit var progressDialog: ProgressDialog
    private val viewModel: EvaluacionViewModel by viewModels()

    override fun getLayoutResourceId(): Int = R.layout.activity_lista_evaluacion
    override fun getActivityTitle(): String = "Lista de Evaluaciones"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        initializeViews()
        setupListeners()
        observeViewModel()
    }

    private fun initializeViews() {
        addsBtn = findViewById(R.id.evAddingBtn)
        recyclerView = findViewById(R.id.evRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        addsBtn.setOnClickListener {
            val intent = Intent(this, EvaluacionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        // Observar evaluaciones por semana
        viewModel.evaluacionesPorSemana.observe(this) { evaluacionesPorSemana ->
            val semanas = evaluacionesPorSemana.keys.toList().sorted()
            semanaAdapter = ListaEvaluacionAdapter(semanas) { semana ->
                val intent = Intent(this, OperarioEvaluacionActivity::class.java)
                intent.putExtra("semana", semana)
                startActivity(intent)
            }
            recyclerView.adapter = semanaAdapter
        }

        // Observar estado de sincronización
        viewModel.syncStatus.observe(this) { status ->
            when (status) {
                is EvaluacionViewModel.SyncStatus.Loading -> {
                    progressDialog = ProgressDialog.show(
                        this,
                        "Sincronizando",
                        "Por favor espere...",
                        true
                    )
                }
                is EvaluacionViewModel.SyncStatus.Success -> {
                    progressDialog.dismiss()
                    Toast.makeText(this, status.message, Toast.LENGTH_SHORT).show()
                }
                is EvaluacionViewModel.SyncStatus.Error -> {
                    progressDialog.dismiss()
                    Toast.makeText(this, status.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }

        // Observar mensajes de error
        viewModel.errorMessage.observe(this) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    // Crear menú de sincronización
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.sync_menu, menu)
        return true
    }

    // Manejar clic en botón de sincronización
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sync -> {
                viewModel.performFullSync()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadEvaluacionesPorSemana()
    }
}