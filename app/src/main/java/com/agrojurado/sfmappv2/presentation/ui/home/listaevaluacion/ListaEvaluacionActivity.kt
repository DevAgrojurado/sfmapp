package com.agrojurado.sfmappv2.presentation.ui.home.listaevaluacion

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.presentation.ui.base.BaseActivity
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.EvaluacionActivity
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.EvaluacionViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ListaEvaluacionActivity : BaseActivity() {
    private lateinit var addsBtn: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var semanaAdapter: SemanaEvaluacionAdapter
    private val viewModel: EvaluacionViewModel by viewModels()

    override fun getLayoutResourceId(): Int = R.layout.activity_lista_evaluacion
    override fun getActivityTitle(): String = "Lista de Evaluaciones"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        viewModel.evaluacionesPorSemana.observe(this) { evaluacionesPorSemana ->
            val semanas = evaluacionesPorSemana.keys.toList().sorted()
            semanaAdapter = SemanaEvaluacionAdapter(semanas) { semana ->
                val intent = Intent(this, EvaluacionesPorSemanaActivity::class.java)
                intent.putExtra("semana", semana)
                startActivity(intent)
            }
            recyclerView.adapter = semanaAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadEvaluacionesPorSemana()
    }
}
