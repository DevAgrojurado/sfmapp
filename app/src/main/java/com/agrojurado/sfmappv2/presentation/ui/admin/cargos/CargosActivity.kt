package com.agrojurado.sfmappv2.presentation.ui.admin.cargos

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.Cargo
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CargosActivity : AppCompatActivity() {
    private lateinit var addsBtn: FloatingActionButton
    private lateinit var recv: RecyclerView
    private lateinit var cargosAdapter: CargosAdapter
    private val viewModel: CargosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cargos)

        addsBtn = findViewById(R.id.addingBtn)
        recv = findViewById(R.id.mRecycler)

        cargosAdapter = CargosAdapter(this, ArrayList()) { cargo, action ->
            when (action) {
                "update" -> updateCargo(cargo)
                "delete" -> deleteCargo(cargo)
            }
        }

        recv.layoutManager = LinearLayoutManager(this)
        recv.adapter = cargosAdapter

        addsBtn.setOnClickListener { addInfo() }

        observeCargos()
    }

    private fun observeCargos() {
        viewModel.cargos.observe(this) { cargos ->
            cargosAdapter.updateCargos(cargos)
        }
    }

    private fun addInfo() {
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.add_item, null)
        val etcargo = v.findViewById<EditText>(R.id.et_cargo)

        val addDialog = AlertDialog.Builder(this)
        addDialog.setView(v)
        addDialog.setPositiveButton("Ok") { dialog, _ ->
            val cargoDescription = etcargo.text.toString()
            if (cargoDescription.isNotEmpty()) {
                lifecycleScope.launch {
                    viewModel.insertCargo(Cargo(descripcion = cargoDescription))
                    Toast.makeText(this@CargosActivity, "Cargo agregado con éxito", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor ingresa un cargo", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        addDialog.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        addDialog.show()
    }

    private fun updateCargo(cargo: Cargo) {
        lifecycleScope.launch {
            viewModel.updateCargo(cargo)
            Toast.makeText(this@CargosActivity, "Cargo actualizado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteCargo(cargo: Cargo) {
        lifecycleScope.launch {
            viewModel.deleteCargo(cargo)
            Toast.makeText(this@CargosActivity, "Cargo eliminado", Toast.LENGTH_SHORT).show()
        }
    }
}