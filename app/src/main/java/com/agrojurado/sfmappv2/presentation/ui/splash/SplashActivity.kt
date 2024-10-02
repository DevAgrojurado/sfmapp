package com.agrojurado.sfmappv2.presentation.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.agrojurado.sfmappv2.presentation.ui.login.LoginActivity
import com.agrojurado.sfmappv2.presentation.ui.login.LoginViewModel
import com.agrojurado.sfmappv2.presentation.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkSession()
    }

    private fun checkSession() {
        lifecycleScope.launch {
            try {
                if (viewModel.isSessionStarted()) {
                    navigateTo(MainActivity::class.java)
                } else {
                    navigateTo(LoginActivity::class.java)
                }
            } catch (e: Exception) {
                // Manejo de errores, puedes logear el error o mostrar un mensaje
                e.printStackTrace()
                navigateTo(LoginActivity::class.java) // Redirigir a Login en caso de error
            } finally {
                finish()
            }
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
    }
}
