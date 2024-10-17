package com.agrojurado.sfmappv2.presentation.ui.admin.usuarios

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agrojurado.sfmappv2.databinding.ActivityUsuariosBinding
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.presentation.ui.crearcuenta.CrearCuentaActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
        observeViewModel()
    }

    private fun initRecyclerView() {
        binding.uRecycler.layoutManager = LinearLayoutManager(this)
        adapter = UsuariosAdapter(this, listOf()) { usuario, action ->
            when (action) {
                //"update" -> updateUsuario(usuario)
                "delete" -> deleteUsuario(usuario)
            }
        }
        binding.uRecycler.adapter = adapter
    }

    private fun initListeners() {
        binding.fabSave.setOnClickListener {
            startActivity(Intent(this, CrearCuentaActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.usuarios.observe(this) { usuarios ->
            adapter.updateUsuarios(usuarios)
        }
    }

    private fun deleteUsuario(usuario: Usuario) {
        lifecycleScope.launch {
            viewModel.deleteUsuario(usuario)
            Toast.makeText(this@UsuariosActivity, "Usuario eliminado", Toast.LENGTH_SHORT).show()
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