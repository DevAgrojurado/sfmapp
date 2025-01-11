package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.operarioevaluacion

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.presentation.ui.base.BaseActivity
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.EvaluacionViewModel
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluaciondetalle.EvaluacionesPorSemanaActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class OperarioEvaluacionActivity : BaseActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OperarioEvaluacionAdapter
    private val viewModel: EvaluacionViewModel by viewModels()
    private var semana: Int = -1

    override fun getLayoutResourceId(): Int = R.layout.activity_evaluaciones_por_semana

    override fun getActivityTitle(): String = "Semana $semana - Polinizacion"

    override fun onCreate(savedInstanceState: Bundle?) {
        semana = intent.getIntExtra("semana", -1)
        if (semana == -1) {
            finish()
            return
        }

        super.onCreate(savedInstanceState)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getActivityTitle()

        setupRecyclerView()
        observeViewModel(semana)
        observeConnectivityState()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rvEvaluacionesPorSemana)
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun observeViewModel(semana: Int) {
        viewModel.operarioMap.observe(this) { operarioMap ->
            handleEvaluaciones(semana, operarioMap)
        }
    }

    private fun handleEvaluaciones(semana: Int, operarioMap: Map<Int, String>) {
        viewModel.evaluacionesPorSemana.observe(this) { evaluacionesPorSemana ->
            val evaluacionesDeSemana = evaluacionesPorSemana[semana] ?: emptyList()

            val evaluacionesPorPolinizador = evaluacionesDeSemana.groupBy {
                it.idPolinizador
            }

            val items = evaluacionesPorPolinizador.map { (polinizadorId, evaluaciones) ->
                ItemOperarioEvaluacion(
                    nombrePolinizador = operarioMap[polinizadorId] ?: "Desconocido",
                    evaluaciones = evaluaciones
                )
            }.sortedBy { it.nombrePolinizador }

            if (!::adapter.isInitialized) {
                adapter = OperarioEvaluacionAdapter(items) { item ->
                    val intent = Intent(this, EvaluacionesPorSemanaActivity::class.java).apply {
                        putExtra("semana", semana)
                        putExtra("idPolinizador", item.evaluaciones.first().idPolinizador)
                        putExtra("nombrePolinizador", item.nombrePolinizador)
                    }
                    startActivity(intent)
                }
                recyclerView.adapter = adapter
            } else {
                adapter.updateItems(items)
            }
        }
    }

    private fun observeConnectivityState() {
        lifecycleScope.launch {
            viewModel.isOnline.collectLatest { isOnline ->
                viewModel.loadEvaluacionesPorSemana()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadEvaluacionesPorSemana()
    }
}