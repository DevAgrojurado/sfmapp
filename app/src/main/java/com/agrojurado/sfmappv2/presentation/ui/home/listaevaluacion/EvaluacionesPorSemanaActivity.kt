package com.agrojurado.sfmappv2.presentation.ui.home.listaevaluacion

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.EvaluacionAdapter
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.EvaluacionDetalleDialog
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.EvaluacionViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EvaluacionesPorSemanaActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EvaluacionAdapter
    private val viewModel: EvaluacionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_evaluaciones_por_semana)

        val semana = intent.getIntExtra("semana", -1)
        if (semana == -1) {
            finish()
            return
        }

        title = "Evaluaciones Semana $semana"

        recyclerView = findViewById(R.id.rvEvaluacionesPorSemana)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = EvaluacionAdapter { evaluacion, nombrePolinizador ->
            showEvaluacionDetalle(evaluacion, nombrePolinizador)
        }
        recyclerView.adapter = adapter

        viewModel.evaluacionesPorSemana.observe(this) { evaluacionesPorSemana ->
            val evaluacionesSemana = evaluacionesPorSemana[semana] ?: emptyList()
            adapter.submitList(evaluacionesSemana)
        }

        viewModel.operarioMap.observe(this) { operarioMap ->
            adapter.setOperarioMap(operarioMap)
        }

        viewModel.loadEvaluacionesPorSemana()
    }

    private fun showEvaluacionDetalle(evaluacion: EvaluacionPolinizacion, nombrePolinizador: String) {
        val nombreEvaluador = viewModel.evaluador.value?.get(evaluacion.idEvaluador) ?: "Desconocido"
        val dialog = EvaluacionDetalleDialog(evaluacion, nombrePolinizador, nombreEvaluador)
        dialog.show(supportFragmentManager, "EvaluacionDetalleDialog")
    }
}