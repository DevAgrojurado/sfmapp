package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.presentation.ui.base.BaseActivity
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EvaluacionActivity : BaseActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private val viewModel: EvaluacionViewModel by viewModels()

    override fun getLayoutResourceId(): Int = R.layout.activity_evaluacion
    override fun getActivityTitle(): String = "Evaluación Polinización"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initViews()
        setupViewPager()
        setupWindowInsets()
        setupObservers()
    }

    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
    }

    private fun setupViewPager() {
        val adapter = EvaluacionPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position + 1) {
                1 -> "Información General"
                2 -> "Detalles Polinizacion"
                else -> "Evaluacion"
            }
        }.attach()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun saveAllData() {
        val adapter = viewPager.adapter as EvaluacionPagerAdapter
        val informacionGeneralFragment = adapter.getFragment(0) as InformacionGeneralFragment
        val detallesPolinizacionFragment = adapter.getFragment(1) as DetallesPolinizacionFragment
        val evaluacionFragment = adapter.getFragment(2) as EvaluacionFragment

        val informacionGeneral = informacionGeneralFragment.getValues()
        val detallesPolinizacion = detallesPolinizacionFragment.getValues()
        val evaluacion = evaluacionFragment.getValues()

        viewModel.saveAllData(informacionGeneral, detallesPolinizacion, evaluacion)
    }

    private fun setupObservers() {
        viewModel.saveResult.observe(this) { success ->
            val message = if (success) "Evaluación guardada exitosamente" else "Por favor llene todos los campos requeridos"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            if (success) finish()
        }

        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.palmExists.observe(this) { exists ->
            if (exists) {
                Toast.makeText(this, "Esta palma ya ha sido registrada", Toast.LENGTH_LONG).show()
            } else {
                saveAllData()
            }
        }
    }

    override fun getToolbarColor(): Int = R.color.green // Replace with your desired color resource
}