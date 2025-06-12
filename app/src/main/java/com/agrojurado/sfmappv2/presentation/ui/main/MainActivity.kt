package com.agrojurado.sfmappv2.presentation.ui.main

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.data.sync.DataSyncManager
import com.agrojurado.sfmappv2.data.sync.SyncWorker
import com.agrojurado.sfmappv2.databinding.ActivityMainBinding
import com.agrojurado.sfmappv2.domain.model.UserRoles
import com.agrojurado.sfmappv2.domain.repository.EvaluacionGeneralRepository
import com.agrojurado.sfmappv2.domain.repository.EvaluacionPolinizacionRepository
import com.agrojurado.sfmappv2.presentation.ui.login.LoginActivity
import com.agrojurado.sfmappv2.presentation.ui.login.LoginViewModel
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var dataSyncManager: DataSyncManager
    @Inject
    lateinit var evaluacionGeneralRepository: EvaluacionGeneralRepository
    @Inject
    lateinit var evaluacionPolinizacionRepository: EvaluacionPolinizacionRepository
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var syncText: TextView
    private lateinit var syncSubtitle: TextView
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        syncText = binding.syncText
        syncSubtitle = binding.syncSubtitle

        // Limpiar evaluaciones temporales al iniciar la aplicación
        if (savedInstanceState == null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    evaluacionGeneralRepository.deleteTemporaryEvaluaciones()
                    Log.d("MainActivity", "Evaluaciones temporales eliminadas al iniciar la aplicación")
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error al eliminar evaluaciones temporales al iniciar: ${e.message}", e)
                }
            }
        } else {
            Log.d("MainActivity", "Restaurando estado, no se eliminan evaluaciones temporales")
        }

        val progressBar: ProgressBar = binding.progressBar
        val progressContainer: View = binding.progressContainer

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_gallery, R.id.nav_admin, R.id.nav_logout),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val rolUsuario = viewModel.obtenerRolUsuarioConSesion()
        if (rolUsuario != UserRoles.ADMINISTRADOR) {
            navView.menu.findItem(R.id.nav_admin).isVisible = false
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    showAlertLogout()
                    true
                }
                R.id.nav_admin -> {
                    if (rolUsuario == UserRoles.ADMINISTRADOR) {
                        val handled = menuItem.onNavDestinationSelected(navController)
                        if (handled) {
                            drawerLayout.closeDrawer(GravityCompat.START)
                        }
                    } else {
                        Toast.makeText(this, "Acceso denegado. No eres administrador.", Toast.LENGTH_SHORT).show()
                        drawerLayout.closeDrawer(GravityCompat.START)
                    }
                    true
                }
                else -> {
                    val handled = menuItem.onNavDestinationSelected(navController)
                    if (handled) {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    }
                    handled
                }
            }
        }

        // Verificar evaluaciones pendientes al iniciar
        checkPendingEvaluations()
        // Observar el flujo de progreso de sincronización
        observeSyncProgress(progressBar, progressContainer)
        // Inicializar la sincronización
        initializeSync(progressBar, progressContainer)
        // Programar el trabajador de sincronización
        scheduleSyncWorker()
    }

    private fun updateSyncText(mainText: String, subtitle: String = "Por favor espere...") {
        runOnUiThread {
            syncText.text = mainText
            syncSubtitle.text = subtitle
        }
    }

    private fun updateSyncBadge(count: Int) {
        runOnUiThread {
            val menu = binding.appBarMain.toolbar.menu
            val syncItem = menu.findItem(R.id.action_sync)
            syncItem?.actionView?.let { actionView ->
                val badgeCount = actionView.findViewById<TextView>(R.id.badge_count)
                if (count > 0) {
                    badgeCount.text = if (count > 99) "99+" else count.toString()
                    badgeCount.visibility = View.VISIBLE
                } else {
                    badgeCount.visibility = View.GONE
                }
            }
        }
    }

    private fun checkPendingEvaluations() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val pendingGeneralCount = withContext(Dispatchers.IO) {
                    evaluacionGeneralRepository.getUnsyncedEvaluationsCount()
                }
                val pendingPolinizacionCount = withContext(Dispatchers.IO) {
                    evaluacionPolinizacionRepository.getUnsyncedEvaluationsCount()
                }
                if (pendingPolinizacionCount > 0) {
                    Log.d(
                        "MainActivity",
                        "Evaluaciones pendientes: $pendingPolinizacionCount (Generales: $pendingGeneralCount, Polinizaciones: $pendingPolinizacionCount)"
                    )
                    updateSyncBadge(pendingPolinizacionCount)
                } else {
                    updateSyncBadge(0)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al verificar evaluaciones pendientes: ${e.message}", e)
                updateSyncBadge(0)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.sync_menu, menu)

        // Configurar el click listener para el badge
        val syncItem = menu.findItem(R.id.action_sync)
        syncItem?.actionView?.setOnClickListener {
            startSyncProcess()
        }

        checkPendingEvaluations() // Actualizar badge al crear el menú
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sync -> {
                startSyncProcess()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAlertLogout() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setIcon(R.drawable.ic_warning)
            .setMessage("¿Está seguro que desea cerrar sesión?")
            .setPositiveButton("Sí") { _, _ -> cerrarSesion() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cerrarSesion() {
        viewModel.cerrarSesion()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun initializeSync(progressBar: ProgressBar, progressContainer: View) {
        CoroutineScope(Dispatchers.Main).launch {
            // Verificar conexión a Internet
            val isNetworkAvailable = withContext(Dispatchers.IO) {
                dataSyncManager.isNetworkAvailable()
            }
            if (!isNetworkAvailable) {
                Toast.makeText(this@MainActivity, "Sin conexión a Internet", Toast.LENGTH_SHORT).show()
                Log.d("MainActivity", "No hay conexión a Internet, no se inicia la sincronización")
                checkPendingEvaluations()
                return@launch
            }

            // Mostrar UI de sincronización solo si hay conexión
            progressContainer.visibility = View.VISIBLE
            progressBar.isIndeterminate = false
            updateSyncText("Cargando datos iniciales...")
            window.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )

            try {
                dataSyncManager.syncAllData(progressBar) {
                    // Actualizar subtítulo después de la sincronización
                    checkPendingEvaluations()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressContainer.visibility = View.GONE
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    Toast.makeText(this@MainActivity, "Error en sincronización: ${e.message}", Toast.LENGTH_LONG).show()
                    checkPendingEvaluations()
                }
            }
        }
    }

    private fun observeSyncProgress(progressBar: ProgressBar, progressContainer: View) {
        lifecycleScope.launch {
            dataSyncManager.syncProgress
                .debounce(50) // Limitar actualizaciones a una cada 50ms
                .collect { syncProgress ->
                    withContext(Dispatchers.Main) {
                        // Calcular el porcentaje
                        val percentage = if (syncProgress.total > 0) {
                            (syncProgress.current * 100) / syncProgress.total
                        } else {
                            0
                        }

                        // Actualizar el ProgressBar
                        progressBar.progress = percentage

                        // Actualizar los textos
                        syncText.text = syncProgress.message
                        syncSubtitle.text = if (syncProgress.total > 0 && syncProgress.total != 100) {
                            "${syncProgress.current} de ${syncProgress.total} ($percentage%)"
                        } else {
                            "Por favor espere..."
                        }

                        // Mostrar el contenedor si no está visible
                        if (progressContainer.visibility != View.VISIBLE) {
                            progressContainer.visibility = View.VISIBLE
                            window.setFlags(
                                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            )
                        }

                        // Ocultar el contenedor si la sincronización está completa, hay un error, o no hay conexión
                        if (syncProgress.message.contains("✅ Sincronización completa") ||
                            syncProgress.message.contains("✅ Cola de sincronización vacía") ||
                            syncProgress.message.startsWith("❌") ||
                            syncProgress.message == "Sin conexión a Internet"
                        ) {
                            progressContainer.visibility = View.GONE
                            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                            Toast.makeText(
                                this@MainActivity,
                                if (syncProgress.message.startsWith("❌")) "Error: ${syncProgress.message}" else "Sincronización completada",
                                Toast.LENGTH_SHORT
                            ).show()
                            checkPendingEvaluations()
                        }
                    }
                }
        }
    }

    private fun scheduleSyncWorker() {
        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            )
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "SyncEvaluacionesWork",
                ExistingPeriodicWorkPolicy.KEEP,
                syncWorkRequest
            )
    }

    private fun startSyncProcess() {
        val progressBar: ProgressBar = binding.progressBar
        val progressContainer: View = binding.progressContainer
        progressContainer.visibility = View.VISIBLE
        progressBar.isIndeterminate = false
        updateSyncText("Preparando sincronización", "Iniciando proceso...")
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )

        CoroutineScope(Dispatchers.Main).launch {
            try {
                dataSyncManager.syncAllData(progressBar) {
                    // Actualizar subtítulo después de la sincronización
                    checkPendingEvaluations()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressContainer.visibility = View.GONE
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    Toast.makeText(this@MainActivity, "Error en sincronización: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("MainActivity", "Error en sincronización: ${e.message}", e)
                    checkPendingEvaluations()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                evaluacionGeneralRepository.deleteTemporaryEvaluaciones()
                Log.d("MainActivity", "Evaluaciones temporales eliminadas al cerrar la aplicación")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al eliminar evaluaciones temporales al cerrar: ${e.message}", e)
            }
        }
    }
}
