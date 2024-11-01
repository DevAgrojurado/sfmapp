package com.agrojurado.sfmappv2.presentation.ui.admin.areas

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.Area
import com.agrojurado.sfmappv2.presentation.ui.base.BaseActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AreasActivity : BaseActivity() {
    private lateinit var addsBtn: FloatingActionButton
    private lateinit var recv: RecyclerView
    private lateinit var areasAdapter: AreasAdapter
    private lateinit var syncButton: FloatingActionButton
    private val viewModel: AreasViewModel by viewModels()

    override fun getLayoutResourceId(): Int = R.layout.activity_areas
    override fun getActivityTitle(): String = "Áreas"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        syncButton = findViewById(R.id.syncButton)
        syncButton.setOnClickListener {
            viewModel.performFullSync()
        }

        initializeViews()
        setupRecyclerView()
        setupListeners()
        observeAreas()
        observeNetworkState()
    }

    private fun initializeViews() {
        addsBtn = findViewById(R.id.addingBtn)
        recv = findViewById(R.id.aRecycler)
    }

    private fun setupRecyclerView() {
        areasAdapter = AreasAdapter(this, ArrayList()) { area, action ->
            when (action) {
                "update" -> updateArea(area)
                "delete" -> deleteArea(area)
            }
        }
        recv.layoutManager = LinearLayoutManager(this)
        recv.adapter = areasAdapter
    }

    private fun setupListeners() {
        addsBtn.setOnClickListener { addInfo() }
    }

    private fun observeNetworkState() {
        lifecycleScope.launch {
            viewModel.isOnline.collect { isOnline ->
                val message = if (isOnline) "Conectado" else "Modo sin conexión"
                Toast.makeText(this@AreasActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun observeAreas() {
        lifecycleScope.launch {
            viewModel.areas.collect { areas ->
                areasAdapter.updateAreas(areas)
            }
        }
    }


    private fun addInfo() {
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.add_area, null)
        val etarea = v.findViewById<EditText>(R.id.et_area)

        val addDialog = AlertDialog.Builder(this)
        addDialog.setView(v)
        addDialog.setPositiveButton("Ok") { dialog, _ ->
            val areaDescription = etarea.text.toString()
            if (areaDescription.isNotEmpty()) {
                lifecycleScope.launch {
                    try {
                        viewModel.insertArea(Area(descripcion = areaDescription))
                        Toast.makeText(this@AreasActivity, "Área agregada con éxito", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@AreasActivity,
                            "Error al guardar: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else {
                Toast.makeText(this, "Por favor ingresa un área", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        addDialog.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        addDialog.show()
    }

    private fun updateArea(area: Area) {
        lifecycleScope.launch {
            viewModel.updateArea(area)
            Toast.makeText(this@AreasActivity, "Área actualizada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteArea(area: Area) {
        lifecycleScope.launch {
            viewModel.deleteArea(area)
            Toast.makeText(this@AreasActivity, "Área eliminada", Toast.LENGTH_SHORT).show()
        }
    }
}