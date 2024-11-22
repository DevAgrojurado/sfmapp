package com.agrojurado.sfmappv2.presentation.ui.home.listaevaluacion

import android.os.Bundle
import android.view.Menu
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.EvaluacionAdapter
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.EvaluacionDetalleDialog
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.EvaluacionViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.agrojurado.sfmappv2.presentation.ui.base.BaseActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class EvaluacionesPorSemanaActivity : BaseActivity() {
    private var _recyclerView: RecyclerView? = null
    private val recyclerView get() = _recyclerView!!
    private var _adapter: EvaluacionAdapter? = null
    private val adapter get() = _adapter!!
    private val viewModel: EvaluacionViewModel by viewModels()
    private var isActivityDestroyed = false

    override fun getLayoutResourceId(): Int = R.layout.activity_evaluaciones_por_semana
    override fun getActivityTitle(): String = "Evaluaciones"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val semana = validateAndGetSemana()
                if (semana != -1) {
                    initializeViews()
                    setupRecyclerView()
                    observeViewModel(semana)
                }
            } catch (e: Exception) {
                Log.e("EvaluacionesActivity", "Error en onCreate: ${e.message}")
                withContext(Dispatchers.Main) {
                    showError("Error al inicializar: ${e.message}")
                    finishSafely()
                }
            }
        }
    }

    private fun validateAndGetSemana(): Int {
        val semana = intent.getIntExtra("semana", -1)
        if (semana == -1) {
            throw IllegalArgumentException("Semana no válida")
        }
        return semana
    }

    private fun initializeViews() {
        _recyclerView = findViewById(R.id.rvEvaluacionesPorSemana)
            ?: throw IllegalStateException("RecyclerView no encontrado")
    }

    private fun setupRecyclerView() {
        if (isActivityDestroyed) return

        _recyclerView?.layoutManager = LinearLayoutManager(this)
        _adapter = EvaluacionAdapter(
            onItemClick = { evaluacion, nombrePolinizador ->
                if (!isActivityDestroyed) {
                    showEvaluacionDetalle(evaluacion, nombrePolinizador)
                }
            },
            onEvaluacionAction = { evaluacion, action ->
                if (!isActivityDestroyed) {
                    handleEvaluacionAction(evaluacion, action)
                }
            }
        )
        _recyclerView?.adapter = _adapter
    }

    private fun handleEvaluacionAction(evaluacion: EvaluacionPolinizacion, action: String) {
        lifecycleScope.launch {
            try {
                when (action) {
                    "delete" -> viewModel.deleteEvaluacion(evaluacion)
                }
            } catch (e: Exception) {
                Log.e("EvaluacionesActivity", "Error en acción: ${e.message}")
                showError("Error al realizar la acción")
            }
        }
    }

    private fun observeViewModel(semana: Int) {
        if (isActivityDestroyed) return

        viewModel.evaluacionesPorSemana.observe(this) { evaluacionesPorSemana ->
            if (!isActivityDestroyed) {
                try {
                    val evaluacionesSemana = evaluacionesPorSemana[semana] ?: emptyList()
                    _adapter?.submitList(evaluacionesSemana)
                } catch (e: Exception) {
                    Log.e("EvaluacionesActivity", "Error al observar evaluaciones: ${e.message}")
                    showError("Error al cargar evaluaciones")
                }
            }
        }

        viewModel.operarioMap.observe(this) { operarioMap ->
            if (!isActivityDestroyed) {
                try {
                    _adapter?.setOperarioMap(operarioMap)
                } catch (e: Exception) {
                    Log.e("EvaluacionesActivity", "Error al observar operarios: ${e.message}")
                    showError("Error al cargar operarios")
                }
            }
        }

        viewModel.loadEvaluacionesPorSemana()
    }

    private fun filterEvaluaciones(query: String) {
        if (isActivityDestroyed) return

        lifecycleScope.launch {
            try {
                val semana = intent.getIntExtra("semana", -1)
                val filteredList = viewModel.evaluacionesPorSemana.value?.get(semana)?.filter { evaluacion ->
                    val nombrePolinizador = viewModel.operarioMap.value?.get(evaluacion.idPolinizador) ?: ""
                    nombrePolinizador.contains(query, ignoreCase = true) ||
                            evaluacion.fecha!!.contains(query, ignoreCase = true) ||
                            evaluacion.hora!!.contains(query, ignoreCase = true) ||
                            evaluacion.observaciones!!.contains(query, ignoreCase = true)
                } ?: emptyList()

                if (!isActivityDestroyed) {
                    _adapter?.submitList(filteredList)
                }
            } catch (e: Exception) {
                Log.e("EvaluacionesActivity", "Error al filtrar: ${e.message}")
                showError("Error al filtrar")
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (isActivityDestroyed) return false

        return try {
            menuInflater.inflate(R.menu.menu_search, menu)
            val searchItem = menu.findItem(R.id.action_search)
            val searchView = searchItem?.actionView as? SearchView
                ?: throw IllegalStateException("SearchView no encontrada")

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = false
                override fun onQueryTextChange(newText: String?): Boolean {
                    if (!isActivityDestroyed) {
                        filterEvaluaciones(newText ?: "")
                    }
                    return true
                }
            })
            true
        } catch (e: Exception) {
            Log.e("EvaluacionesActivity", "Error en menu: ${e.message}")
            false
        }
    }

    private fun showEvaluacionDetalle(evaluacion: EvaluacionPolinizacion, nombrePolinizador: String) {
        if (isActivityDestroyed) return

        try {
            val nombreEvaluador = viewModel.evaluador.value?.get(evaluacion.idEvaluador) ?: "Desconocido"
            val dialog = EvaluacionDetalleDialog(evaluacion, nombrePolinizador, nombreEvaluador)
            dialog.show(supportFragmentManager, "EvaluacionDetalleDialog")
        } catch (e: Exception) {
            Log.e("EvaluacionesActivity", "Error al mostrar diálogo: ${e.message}")
            showError("Error al mostrar detalle")
        }
    }

    private fun showError(message: String) {
        if (!isActivityDestroyed) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun finishSafely() {
        isActivityDestroyed = true
        finish()
    }

    override fun onDestroy() {
        isActivityDestroyed = true
        _recyclerView = null
        _adapter = null
        super.onDestroy()
    }
}