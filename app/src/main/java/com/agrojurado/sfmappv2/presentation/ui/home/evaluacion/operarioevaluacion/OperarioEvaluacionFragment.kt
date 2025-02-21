package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.operarioevaluacion

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.agrojurado.sfmappv2.databinding.FragmentListaEvaluacionBinding
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluacionfragmentsform.EvaluacionActivity
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluacionfragmentsform.EvaluacionViewModel
import com.agrojurado.sfmappv2.utils.ExcelUtils.exportToExcel
import com.agrojurado.sfmappv2.utils.PdfUtils.exportPdf
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OperarioEvaluacionFragment : Fragment() {
    private var _binding: FragmentListaEvaluacionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EvaluacionViewModel by viewModels()
    private val args: OperarioEvaluacionFragmentArgs by navArgs()
    private lateinit var adapter: OperarioEvaluacionAdapter
    private var isInitialLoad = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListaEvaluacionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInitialViewState()
        setupRecyclerView()
        setupObservers()
        setupListeners()

        if (isInitialLoad) {
            loadData()
            isInitialLoad = false
        }
    }

    private fun setupInitialViewState() {
        binding.apply {
            rvEvaluacion.visibility = View.GONE
            loadingIndicator?.visibility = View.VISIBLE
            btnExportAllPdf.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {
        adapter = OperarioEvaluacionAdapter(
            onItemClick = { idPolinizador, nombrePolinizador ->
                findNavController().navigate(
                    OperarioEvaluacionFragmentDirections
                        .actionOperarioEvaluacionToEvaluacionDetalle(
                            semana = args.semana,
                            idPolinizador = idPolinizador,
                            nombrePolinizador = nombrePolinizador
                        )
                )
            },
            onExportPdfClick = { evaluaciones, nombreOperario ->
                if (evaluaciones.isNotEmpty()) {
                    exportPdf(
                        evaluaciones = evaluaciones,
                        evaluadorMap = viewModel.evaluador.value ?: emptyMap(),
                        operarioMap = viewModel.operarioMap.value ?: emptyMap(),
                        loteMap = viewModel.loteMap.value ?: emptyMap()
                    )
                    Toast.makeText(
                        requireContext(),
                        "Exportando evaluaciones de $nombreOperario",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No hay evaluaciones para exportar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onExportExcelClick = { evaluaciones, nombreOperario ->
                if (evaluaciones.isNotEmpty()) {
                    exportToExcel(
                        evaluaciones = evaluaciones,
                        evaluadorMap = viewModel.evaluador.value ?: emptyMap(),
                        operarioMap = viewModel.operarioMap.value ?: emptyMap(),
                        loteMap = viewModel.loteMap.value ?: emptyMap()
                    )
                    Toast.makeText(
                        requireContext(),
                        "Exportando a Excel evaluaciones de $nombreOperario",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No hay evaluaciones para exportar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            countUniquePalms = { evaluaciones ->
                viewModel.countUniquePalms(evaluaciones)
            },
            getEvaluadorMap = { viewModel.evaluador.value ?: emptyMap() },
            getLoteMap = { viewModel.loteMap.value ?: emptyMap() }
        )
        binding.rvEvaluacion.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@OperarioEvaluacionFragment.adapter
            visibility = View.GONE
        }
    }

    private fun setupListeners() {
        binding.evAddingBtn.setOnClickListener {
            val intent = Intent(requireContext(), EvaluacionActivity::class.java)
            startActivity(intent)
        }

        binding.swipeRefresh?.setOnRefreshListener {
            loadData()
        }
    }

    private fun setupObservers() {
        // Observar estado de carga
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                updateLoadingState(isLoading)
            }
        }

        // Observar mapa de operarios
        viewModel.operarioMap.observe(viewLifecycleOwner) { operarioMap ->
            updateEvaluacionesDisplay(operarioMap)
        }

        // Observar evaluaciones por semana
        viewModel.evaluacionesPorSemana.observe(viewLifecycleOwner) { evaluacionesPorSemana ->
            viewModel.operarioMap.value?.let { operarioMap ->
                updateEvaluacionesDisplay(operarioMap, evaluacionesPorSemana)
            }
        }

        // Observar mensajes de error
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
                updateLoadingState(false)
            }
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        binding.apply {
            loadingIndicator?.visibility = if (isLoading) View.VISIBLE else View.GONE

            if (!isLoading) {
                rvEvaluacion.visibility = if (adapter.itemCount > 0) View.VISIBLE else View.GONE
                swipeRefresh?.isRefreshing = false
            } else {
                rvEvaluacion.visibility = View.GONE
            }
        }
    }

    private fun updateEvaluacionesDisplay(
        operarioMap: Map<Int, String>,
        evaluacionesPorSemana: Map<Int, List<EvaluacionPolinizacion>> = viewModel.evaluacionesPorSemana.value ?: emptyMap()
    ) {
        val evaluacionesDeSemana = evaluacionesPorSemana[args.semana] ?: emptyList()

        val evaluacionesPorPolinizador = evaluacionesDeSemana
            .groupBy { Pair(it.idPolinizador, operarioMap[it.idPolinizador] ?: "Desconocido") }
            .toSortedMap(compareBy { it.second })

        adapter.updateItems(evaluacionesPorPolinizador)

        // Actualizar visibilidad después de cargar datos
        binding.rvEvaluacion.visibility = if (evaluacionesPorPolinizador.isNotEmpty()) View.VISIBLE else View.GONE
        updateLoadingState(false)
    }

    private fun loadData() {
        updateLoadingState(true)
        viewModel.loadEvaluacionesPorSemana()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}