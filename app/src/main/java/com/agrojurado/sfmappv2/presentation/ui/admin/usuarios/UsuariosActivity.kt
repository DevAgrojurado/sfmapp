package com.agrojurado.sfmappv2.presentation.ui.admin.usuarios

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.agrojurado.sfmappv2.databinding.ActivityUsuariosBinding
import com.agrojurado.sfmappv2.presentation.ui.crearcuenta.CrearCuentaActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UsuariosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsuariosBinding
    private lateinit var adapter: UsuariosAdapter
    private val viewModel: UsuariosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsuariosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initRecyclerView()
        initListeners()
        observeViewModel() // Observa el ViewModel
        //viewModel.usuarios // Llama a la función para obtener usuarios
    }

    private fun initRecyclerView() {
        binding.uRecycler.layoutManager = LinearLayoutManager(this)
        adapter = UsuariosAdapter(listOf())
        binding.uRecycler.adapter = adapter
    }

    private fun initListeners() {
        binding.fabSave.setOnClickListener {
            startActivity(Intent(this, CrearCuentaActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.usuarios.observe(this) { usuarios ->
            adapter.updateUsuarios(usuarios) // Actualiza el adaptador con la nueva lista
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
