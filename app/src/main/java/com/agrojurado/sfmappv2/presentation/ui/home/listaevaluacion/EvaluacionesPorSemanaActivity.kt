package com.agrojurado.sfmappv2.presentation.ui.home.listaevaluacion

import android.os.Bundle
import android.view.Menu
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.EvaluacionAdapter
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.EvaluacionDetalleDialog
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.EvaluacionViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.agrojurado.sfmappv2.presentation.ui.base.BaseActivity

@AndroidEntryPoint
class EvaluacionesPorSemanaActivity : BaseActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EvaluacionAdapter
    private val viewModel: EvaluacionViewModel by viewModels()

    override fun getLayoutResourceId(): Int = R.layout.activity_evaluaciones_por_semana
    override fun getActivityTitle(): String = "Evaluaciones"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val semana = intent.getIntExtra("semana", -1)
        if (semana == -1) {
            finish()
            return
        }
        initializeViews()
        setupRecyclerView()
        observeViewModel(semana)
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.rvEvaluacionesPorSemana)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = EvaluacionAdapter { evaluacion, nombrePolinizador ->
            showEvaluacionDetalle(evaluacion, nombrePolinizador)
        }
        recyclerView.adapter = adapter
    }

    private fun observeViewModel(semana: Int) {
        viewModel.evaluacionesPorSemana.observe(this) { evaluacionesPorSemana ->
            val evaluacionesSemana = evaluacionesPorSemana[semana] ?: emptyList()
            adapter.submitList(evaluacionesSemana)
        }

        viewModel.operarioMap.observe(this) { operarioMap ->
            adapter.setOperarioMap(operarioMap)
        }

        viewModel.loadEvaluacionesPorSemana()
    }

    private fun filterEvaluaciones(query: String) {
        val semana = intent.getIntExtra("semana", -1)
        val filteredList = viewModel.evaluacionesPorSemana.value?.get(semana)?.filter { evaluacion ->
            val nombrePolinizador = viewModel.operarioMap.value?.get(evaluacion.idPolinizador) ?: ""
            nombrePolinizador.contains(query, ignoreCase = true) ||
                    evaluacion.fecha.contains(query, ignoreCase = true) ||
                    evaluacion.hora.contains(query, ignoreCase = true) ||
                    evaluacion.observaciones.contains(query, ignoreCase = true)
        } ?: emptyList()
        adapter.submitList(filteredList)
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
                filterEvaluaciones(newText ?: "")
                return true
            }
        })
        return true
    }

    private fun showEvaluacionDetalle(evaluacion: EvaluacionPolinizacion, nombrePolinizador: String) {
        val nombreEvaluador = viewModel.evaluador.value?.get(evaluacion.idEvaluador) ?: "Desconocido"
        val dialog = EvaluacionDetalleDialog(evaluacion, nombrePolinizador, nombreEvaluador)
        dialog.show(supportFragmentManager, "EvaluacionDetalleDialog")
    }
}