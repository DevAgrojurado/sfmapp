package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion

import android.os.Bundle
import android.view.View
import android.widget.Button
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
    private lateinit var btnBack: Button
    private lateinit var btnForward: Button
    private val viewModel: EvaluacionViewModel by viewModels()
    private var isSaving = false

    override fun getLayoutResourceId(): Int = R.layout.activity_evaluacion
    override fun getActivityTitle(): String = "Evaluación Polinización"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initViews()
        setupViewPager()
        setupWindowInsets()
        setupObservers()
        setupNavigation()
    }

    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
    }

    private fun setupViewPager() {
        val adapter = EvaluacionPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = true // Permitir deslizar entre páginas

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Información General"
                1 -> "Detalles Polinizacion"
                2 -> "Evaluacion"
                else -> ""
            }
        }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateNavigationButtons(position)
            }
        })
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupNavigation() {
        btnBack.setOnClickListener {
            if (!isSaving) {
                when (viewPager.currentItem) {
                    0 -> finish() // Go back to previous activity
                    else -> viewPager.currentItem = viewPager.currentItem - 1
                }
            }
        }

        btnForward.setOnClickListener {
            if (!isSaving) {
                if (viewPager.currentItem < 2) {
                    viewPager.currentItem = viewPager.currentItem + 1
                } else {
                    startSaveProcess()
                }
            }
        }

        updateNavigationButtons(0)
    }

    private fun startSaveProcess() {
        if (!isSaving) {
            isSaving = true
            btnForward.isEnabled = false
            btnBack.isEnabled = false
            viewPager.isUserInputEnabled = false // Deshabilitar deslizamiento durante el guardado
            saveAllData()
        }
    }

    private fun updateNavigationButtons(position: Int) {
        when (position) {
            0 -> {
                btnBack.text = "Cerrar"
                btnForward.text = "Siguiente"
                btnForward.isEnabled = !isSaving
            }
            1 -> {
                btnBack.text = "Atrás"
                btnForward.text = "Siguiente"
                btnForward.isEnabled = !isSaving
            }
            2 -> {
                btnBack.text = "Atrás"
                btnForward.text = "Guardar"
                btnForward.isEnabled = !isSaving
            }
        }
    }

    private fun saveAllData() {
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
            if (success) {
                Toast.makeText(this, "Evaluación guardada exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Por favor llene todos los campos requeridos", Toast.LENGTH_SHORT).show()
                resetSaveState()
            }
        }

        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                resetSaveState()
            }
        }

        viewModel.isSaving.observe(this) { saving ->
            isSaving = saving
            btnForward.isEnabled = !saving
            btnBack.isEnabled = !saving
            viewPager.isUserInputEnabled = !saving
        }
    }

    private fun resetSaveState() {
        isSaving = false
        btnForward.isEnabled = true
        btnBack.isEnabled = true
        viewPager.isUserInputEnabled = true
        updateNavigationButtons(viewPager.currentItem)
    }

    override fun getToolbarColor(): Int = R.color.green
}