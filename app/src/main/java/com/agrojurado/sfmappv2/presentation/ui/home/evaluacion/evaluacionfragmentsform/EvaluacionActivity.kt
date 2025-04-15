package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluacionfragmentsform

import android.os.Bundle
import android.os.Parcel
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.presentation.ui.base.BaseActivity
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.shared.SharedSelectionViewModel
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
        savedInstanceState?.let {
            Log.d("EvaluacionActivity", "Restored state keys: ${it.keySet()}")
            it.keySet().forEach { key ->
                Log.d("EvaluacionActivity", "Key: $key, Value: ${it.get(key)}")
            }
        }

        initViews()
        setupViewPager()
        setupWindowInsets()
        setupObservers()
        setupNavigation()

        val evaluacionGeneralId = intent.getIntExtra("evaluacionGeneralId", -1)
        val operarioId = intent.getIntExtra("operarioId", -1).let { if (it == -1) null else it }
        val loteId = intent.getIntExtra("loteId", -1).let { if (it == -1) null else it }
        val seccion = intent.getIntExtra("seccion", -1).let { if (it == -1) null else it }

        if (evaluacionGeneralId == -1) {
            Log.e("EvaluacionActivity", "⚠ Evaluacion General ID is invalid")
            Toast.makeText(this, "Error: Invalid general evaluation data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel.setEvaluacionGeneralId(evaluacionGeneralId)
        Log.d("EvaluacionActivity", "✅ Evaluacion General ID received: $evaluacionGeneralId")

        val sharedViewModel: SharedSelectionViewModel by viewModels()
        sharedViewModel.ensureDataLoaded()
        sharedViewModel.applySelections(operarioId, loteId, seccion)

        savedInstanceState?.let {
            val currentPage = it.getInt("currentPage", 0)
            viewPager.setCurrentItem(currentPage, false)
        }
    }

    private fun initViews() {
        viewPager = findViewById(R.id.viewPager) ?: throw IllegalStateException("ViewPager not found")
        tabLayout = findViewById(R.id.tabLayout) ?: throw IllegalStateException("TabLayout not found")
        btnBack = findViewById(R.id.btnBack) ?: throw IllegalStateException("btnBack not found")
        btnForward = findViewById(R.id.btnForward) ?: throw IllegalStateException("btnForward not found")
    }

    private fun setupViewPager() {
        val evaluacionGeneralId = intent.getIntExtra("evaluacionGeneralId", -1)
        val operarioId = intent.getIntExtra("operarioId", -1).let { if (it == -1) null else it }
        val loteId = intent.getIntExtra("loteId", -1).let { if (it == -1) null else it }
        val seccion = intent.getIntExtra("seccion", -1).let { if (it == -1) null else it }

        val adapter = EvaluacionPagerAdapter(this, evaluacionGeneralId, operarioId, loteId, seccion)
        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = true
        viewPager.isSaveEnabled = false

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Información General"
                1 -> "Detalles Polinización"
                2 -> "Evaluación"
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
                    0 -> finish()
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
            viewPager.isUserInputEnabled = false
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
        val fragmentManager = supportFragmentManager
        val informacionGeneralFragment = fragmentManager.findFragmentByTag("f0") as? InformacionGeneralFragment
        val detallesPolinizacionFragment = fragmentManager.findFragmentByTag("f1") as? DetallesPolinizacionFragment
        val evaluacionFragment = fragmentManager.findFragmentByTag("f2") as? EvaluacionFragment

        if (informacionGeneralFragment == null || detallesPolinizacionFragment == null || evaluacionFragment == null) {
            Log.e("EvaluacionActivity", "One or more fragments not found")
            Toast.makeText(this, "Error: No se encontraron todos los fragmentos", Toast.LENGTH_SHORT).show()
            resetSaveState()
            return
        }

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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentPage", viewPager.currentItem)
        Log.d("EvaluacionActivity", "Saving only currentPage: ${viewPager.currentItem}, skipping full state")
        val bundleSize = getBundleSize(outState)
        Log.d("EvaluacionActivity", "Bundle size: $bundleSize bytes")
        if (bundleSize > 500 * 1024) { // Límite de 500 KB
            Log.w("EvaluacionActivity", "Bundle size exceeded ($bundleSize bytes), clearing excess data")
            outState.clear()
            outState.putInt("currentPage", viewPager.currentItem)
        }
    }

    private fun getBundleSize(bundle: Bundle): Long {
        val parcel = Parcel.obtain()
        try {
            parcel.writeBundle(bundle)
            return parcel.dataSize().toLong()
        } finally {
            parcel.recycle()
        }
    }
}