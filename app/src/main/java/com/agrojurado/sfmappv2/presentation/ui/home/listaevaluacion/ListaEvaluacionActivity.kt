package com.agrojurado.sfmappv2.presentation.ui.home.listaevaluacion

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.EvaluacionActivity
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.EvaluacionAdapter
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.EvaluacionDetalleDialog
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.EvaluacionViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ListaEvaluacionActivity : AppCompatActivity() {
    private lateinit var addsBtn: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EvaluacionAdapter
    private val viewModel: EvaluacionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lista_evaluacion)

        addsBtn = findViewById(R.id.evAddingBtn)
        recyclerView = findViewById(R.id.evRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = EvaluacionAdapter { evaluacion, nombrePolinizador ->
            showEvaluacionDetalle(evaluacion, nombrePolinizador)
        }
        recyclerView.adapter = adapter

        viewModel.evaluaciones.observe(this) { evaluaciones ->
            adapter.submitList(evaluaciones)
        }

        viewModel.operarioMap.observe(this) { operarioMap ->
            adapter.setOperarioMap(operarioMap)
        }

        addsBtn.setOnClickListener {
            val intent = Intent(this, EvaluacionActivity::class.java)
            startActivity(intent)
        }

        viewModel.loadEvaluaciones()
    }

    private fun showEvaluacionDetalle(evaluacion: EvaluacionPolinizacion, nombrePolinizador: String) {
        val dialog = EvaluacionDetalleDialog(evaluacion, nombrePolinizador)
        dialog.show(supportFragmentManager, "EvaluacionDetalleDialog")
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadEvaluaciones()
    }
}