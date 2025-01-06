package com.agrojurado.sfmappv2.presentation.ui.main

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
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
import com.agrojurado.sfmappv2.data.workers.SyncEvaluacionesWorker
import com.agrojurado.sfmappv2.databinding.ActivityMainBinding
import com.agrojurado.sfmappv2.domain.model.UserRoles
import com.agrojurado.sfmappv2.presentation.ui.login.LoginActivity
import com.agrojurado.sfmappv2.presentation.ui.login.LoginViewModel
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var dataSyncManager: DataSyncManager
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val progressBar: ProgressBar = binding.appBarMain.progressBar

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

        // Comprobamos el rol del usuario antes de permitir el acceso a la sección Admin
        val rolUsuario = viewModel.obtenerRolUsuarioConSesion()

        // Ocultar el ítem del menú de administrador si el usuario no es administrador
        if (rolUsuario != UserRoles.ADMINISTRADOR) {
            navView.menu.findItem(R.id.nav_admin).isVisible = false
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    // Mostrar un cuadro de diálogo para confirmar el cierre de sesión
                    showAlertLogout()
                    true
                }
                R.id.nav_admin -> {
                    // Comprobamos si el usuario tiene rol de ADMINISTRADOR antes de acceder a la sección Admin
                    if (rolUsuario == UserRoles.ADMINISTRADOR) {
                        // Si es administrador, navegamos al AdminFragment
                        val handled = menuItem.onNavDestinationSelected(navController)
                        if (handled) {
                            drawerLayout.closeDrawer(GravityCompat.START)  // Cerramos el DrawerLayout
                        }
                    } else {
                        // Si no es administrador, mostramos un mensaje de acceso denegado
                        Toast.makeText(this, "Acceso denegado. No eres administrador.", Toast.LENGTH_SHORT).show()
                        drawerLayout.closeDrawer(GravityCompat.START)  // Cerramos el DrawerLayout sin navegar
                    }
                    true
                }
                else -> {
                    // Para otros elementos del menú, manejamos la navegación normalmente
                    val handled = menuItem.onNavDestinationSelected(navController)
                    if (handled) {
                        drawerLayout.closeDrawer(GravityCompat.START)  // Cerramos el DrawerLayout
                    }
                    handled
                }
            }
        }

        // Inicializar la sincronización con barra de progreso
        initializeSync(progressBar)
        dataSyncManager.autoSyncOnReconnect()

        // Programar el worker
        scheduleSyncWorker()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.sync_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sync -> {
                val progressBar: ProgressBar = binding.appBarMain.progressBar
                progressBar.visibility = View.VISIBLE
                dataSyncManager.syncAllData(progressBar) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Sincronización completada", Toast.LENGTH_SHORT).show()
                    }
                }
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

    private fun initializeSync(progressBar: ProgressBar) {
        dataSyncManager.syncAllData(progressBar) {
            runOnUiThread {
                Toast.makeText(this, "Sincronización completada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun scheduleSyncWorker() {
        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncEvaluacionesWorker>(15, TimeUnit.MINUTES)
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

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
