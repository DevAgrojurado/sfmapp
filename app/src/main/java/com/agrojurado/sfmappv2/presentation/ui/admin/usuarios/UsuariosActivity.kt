package com.agrojurado.sfmappv2.presentation.ui.admin.usuarios

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.databinding.ActivityUsuariosBinding
import com.agrojurado.sfmappv2.domain.model.Usuario
import com.agrojurado.sfmappv2.presentation.ui.crearcuenta.CreateUserActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UsuariosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsuariosBinding
    private lateinit var adapter: UsuariosAdapter
    private lateinit var syncButton: FloatingActionButton
    private val viewModel: UsuariosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsuariosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initializeViews()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        observeNetworkState()
    }

    private fun initializeViews() {
        syncButton = findViewById(R.id.syncButtonU) // Asegúrate de que este ID esté en tu layout
    }

    private fun setupRecyclerView() {
        binding.uRecycler.layoutManager = LinearLayoutManager(this)
        adapter = UsuariosAdapter(this, listOf()) { usuario, action ->
            when (action) {
                "delete" -> deleteUsuario(usuario)
                "edit" -> editUsuario(usuario)
            }
        }
        binding.uRecycler.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabSave.setOnClickListener {
            startActivity(Intent(this, CreateUserActivity::class.java))
        }

        // Listener para el botón de sincronización
        syncButton.setOnClickListener {
            viewModel.performFullSync()
        }
    }

    private fun observeViewModel() {
        // Observar la lista de usuarios
        viewModel.usuarios.observe(this) { usuarios ->
            adapter.updateUsuarios(usuarios)  // Actualizar el RecyclerView con la nueva lista
        }

        // Observar los errores
        lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(this@UsuariosActivity, it, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Observar el estado de carga
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                // Mostrar/ocultar un indicador de carga si es necesario
            }
        }
    }

    private fun observeNetworkState() {
        lifecycleScope.launch {
            viewModel.isOnline.collect { isOnline ->
                val message = if (isOnline) "Conectado" else "Modo sin conexión"
                Toast.makeText(this@UsuariosActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteUsuario(usuario: Usuario) {
        lifecycleScope.launch {
            viewModel.deleteUsuario(usuario)

            // Observamos el error y mostramos el AlertDialog si es necesario
            viewModel.error.collect { error ->
                error?.let {
                    if (it.contains("No hay conexión a Internet")) {
                        showNoConnectionAlert()
                    } else {
                        showErrorAlert(it)
                    }
                }
            }
        }
    }

    private fun editUsuario(usuario: Usuario) {
        lifecycleScope.launch {
            viewModel.updateUsuario(usuario)

            // Observamos el error y mostramos el AlertDialog si es necesario
            viewModel.error.collect { error ->
                error?.let {
                    if (it.contains("No hay conexión a Internet")) {
                        showNoConnectionAlert()
                    } else {
                        showErrorAlert(it)
                    }
                }
            }
        }
    }

    private fun showNoConnectionAlert() {
        AlertDialog.Builder(this@UsuariosActivity).apply {
            setTitle("Sin conexión a Internet")
            setMessage("No se puede eliminar el usuario porque no hay conexión a Internet.")
            setPositiveButton("Aceptar") { dialog, _ -> dialog.dismiss() }
            setCancelable(false)  // Evitar que se cierre tocando fuera del cuadro de diálogo
            show()  // Mostrar el diálogo
        }
    }

    private fun showErrorAlert(message: String) {
        AlertDialog.Builder(this@UsuariosActivity).apply {
            setTitle("Error")
            setMessage(message)
            setPositiveButton("Aceptar") { dialog, _ -> dialog.dismiss() }
            setCancelable(false)
            show()
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
