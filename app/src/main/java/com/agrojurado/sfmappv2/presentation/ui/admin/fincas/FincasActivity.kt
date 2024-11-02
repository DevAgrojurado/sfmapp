package com.agrojurado.sfmappv2.presentation.ui.admin.fincas

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
import com.agrojurado.sfmappv2.domain.model.Finca
import com.agrojurado.sfmappv2.presentation.ui.base.BaseActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FincasActivity : BaseActivity() {
    private lateinit var addsBtn: FloatingActionButton
    private lateinit var recv: RecyclerView
    private lateinit var fincasAdapter: FincasAdapter
    private lateinit var syncButton: FloatingActionButton
    private val viewModel: FincasViewModel by viewModels()

    override fun getLayoutResourceId(): Int = R.layout.activity_fincas
    override fun getActivityTitle(): String = "Fincas"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        syncButton = findViewById(R.id.syncButtonF)
        syncButton.setOnClickListener {
            viewModel.performFullSync()
        }

        initializeViews()
        setupRecyclerView()
        setupListeners()
        observeFincas()
        observeNetworkState()
    }

    private fun initializeViews() {
        addsBtn = findViewById(R.id.addingBtn)
        recv = findViewById(R.id.mRecycler)
    }

    private fun setupRecyclerView() {
        fincasAdapter = FincasAdapter(this, ArrayList()) { finca, action ->
            when (action) {
                "update" -> updateFinca(finca)
                "delete" -> deleteFinca(finca)
            }
        }
        recv.layoutManager = LinearLayoutManager(this)
        recv.adapter = fincasAdapter
    }

    private fun setupListeners() {
        addsBtn.setOnClickListener { addInfo() }
    }

    private fun observeNetworkState() {
        lifecycleScope.launch {
            viewModel.isOnline.collect { isOnline ->
                val message = if (isOnline) "Conectado" else "Modo sin conexión"
                Toast.makeText(this@FincasActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeFincas() {
        lifecycleScope.launch {
            viewModel.fincas.collect { fincas ->
                fincasAdapter.updateFincas(fincas)
            }
        }
    }

    private fun addInfo() {
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.add_finca, null)
        val etFinca = v.findViewById<EditText>(R.id.et_finca)

        val addDialog = AlertDialog.Builder(this)
        addDialog.setView(v)
        addDialog.setPositiveButton("Ok") { dialog, _ ->
            val fincaDescription = etFinca.text.toString()
            if (fincaDescription.isNotEmpty()) {
                lifecycleScope.launch {
                    try {
                        viewModel.insertFinca(Finca(descripcion = fincaDescription))
                        Toast.makeText(this@FincasActivity, "Finca agregada con éxito", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@FincasActivity,
                            "Error al guardar: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else {
                Toast.makeText(this, "Por favor ingresa una finca", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        addDialog.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        addDialog.show()
    }

    private fun updateFinca(finca: Finca) {
        lifecycleScope.launch {
            viewModel.updateFinca(finca)
            Toast.makeText(this@FincasActivity, "Finca actualizada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteFinca(finca: Finca) {
        lifecycleScope.launch {
            viewModel.deleteFinca(finca)
            Toast.makeText(this@FincasActivity, "Finca eliminada", Toast.LENGTH_SHORT).show()
        }
    }
}