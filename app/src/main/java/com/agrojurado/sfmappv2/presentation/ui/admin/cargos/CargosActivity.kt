package com.agrojurado.sfmappv2.presentation.ui.admin.cargos

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.domain.model.Cargo
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CargosActivity : AppCompatActivity() {
    private lateinit var addsBtn: FloatingActionButton
    private lateinit var recv: RecyclerView
    private lateinit var cargosList: ArrayList<Cargo>
    private lateinit var cargosAdapter: CargosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cargos)

        cargosList = ArrayList()

        addsBtn = findViewById(R.id.addingBtn)
        recv = findViewById(R.id.mRecycler)

        cargosAdapter = CargosAdapter(this, cargosList)

        // Configurar el RecyclerView
        recv.layoutManager = LinearLayoutManager(this)
        recv.adapter = cargosAdapter

        addsBtn.setOnClickListener { addInfo() }
    }

    private fun addInfo() {
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.add_item, null)

        val etcargo = v.findViewById<EditText>(R.id.et_cargo)

        val addDialog = AlertDialog.Builder(this)
        addDialog.setView(v)
        addDialog.setPositiveButton("Ok") { dialog, _->
            val cargoDescription = etcargo.text.toString()
            if (cargoDescription.isNotEmpty()) {
                val newCargo = Cargo(descripcion = cargoDescription)
                cargosList.add(newCargo)
                cargosAdapter.notifyDataSetChanged()
                Toast.makeText(this, "Cargo agregado con éxito", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Por favor ingresa un cargo", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        addDialog.create()
        addDialog.show()
    }
}