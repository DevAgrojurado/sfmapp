package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.listaevaluacion

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.databinding.FragmentListaEvaluacionBinding
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluacionfragmentsform.EvaluacionActivity
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluacionfragmentsform.EvaluacionViewModel
import com.agrojurado.sfmappv2.utils.PdfUtils.exportPdf
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ListaEvaluacionFragment : Fragment() {
    private var _binding: FragmentListaEvaluacionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EvaluacionViewModel by viewModels()
    private lateinit var semanaAdapter: ListaEvaluacionAdapter
    private var isInitialLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

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
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        observeLoadingState()

        // Iniciar carga solo si es la primera vez
        if (isInitialLoad) {
            viewModel.loadEvaluacionesPorSemana()
            isInitialLoad = false
        }
    }

    private fun observeLoadingState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.loadingIndicator?.visibility = if (isLoading) View.VISIBLE else View.GONE
                if (!isLoading && semanaAdapter.itemCount > 0) {
                    binding.rvEvaluacion.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupRecyclerView() {
        semanaAdapter = ListaEvaluacionAdapter(emptyList(),
            onItemClick = { semana ->
                findNavController().navigate(
                    ListaEvaluacionFragmentDirections
                        .actionListaEvaluacionToOperarioEvaluacionFragment(semana)
                )
            },
            onExportPdfClick = { semana ->
                val evaluaciones = viewModel.evaluacionesPorSemana.value?.get(semana) ?: emptyList()
                if (evaluaciones.isNotEmpty()) {
                    exportPdf(
                        evaluaciones = evaluaciones,
                        evaluadorMap = viewModel.evaluador.value ?: emptyMap(),
                        operarioMap = viewModel.operarioMap.value ?: emptyMap(),
                        loteMap = viewModel.loteMap.value ?: emptyMap()
                    )
                    Toast.makeText(
                        requireContext(),
                        "Exportando evaluaciones de Semana $semana",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No hay evaluaciones para exportar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
        binding.rvEvaluacion.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvaluacion.adapter = semanaAdapter
    }

    private fun setupListeners() {
        binding.evAddingBtn.setOnClickListener {
            val intent = Intent(requireContext(), EvaluacionActivity::class.java)
            startActivity(intent)
        }

        binding.swipeRefresh?.setOnRefreshListener {
            viewModel.loadEvaluacionesPorSemana()
            binding.swipeRefresh?.isRefreshing = false
        }

        binding.btnExportAllPdf.setOnClickListener {
            val evaluaciones =
                viewModel.evaluacionesPorSemana.value?.values?.flatten() ?: emptyList()
            if (evaluaciones.isNotEmpty()) {
                exportPdf(
                    evaluaciones = evaluaciones,
                    evaluadorMap = viewModel.evaluador.value ?: emptyMap(),
                    operarioMap = viewModel.operarioMap.value ?: emptyMap(),
                    loteMap = viewModel.loteMap.value ?: emptyMap()
                )
            } else {
                Toast.makeText(
                    requireContext(),
                    "No hay evaluaciones para exportar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.evaluacionesPorSemana.observe(viewLifecycleOwner) { evaluacionesPorSemana ->
            updateEvaluacionesDisplay(evaluacionesPorSemana)
            hideLoadingIndicators()
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
                hideLoadingIndicators()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                if (!isLoading) {
                    hideLoadingIndicators()
                }
            }
        }
    }

    private fun updateEvaluacionesDisplay(evaluacionesPorSemana: Map<Int, List<EvaluacionPolinizacion>>) {
        try {
            val semanas = evaluacionesPorSemana.keys.toList().sorted()

            if (semanas.isEmpty()) {
                binding.rvEvaluacion.visibility = View.GONE
                // Optionally show empty state
                return
            }

            semanaAdapter.updateItems(semanas)
            binding.rvEvaluacion.visibility = View.VISIBLE
            hideLoadingIndicators()
        } catch (e: Exception) {
            Log.e("ListaEvaluacionFragment", "Error updating display: ${e.message}")
            Toast.makeText(requireContext(), "Error al actualizar la lista", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideLoadingIndicators() {
        binding.loadingIndicator?.visibility = View.GONE
        binding.rvEvaluacion.visibility = if (semanaAdapter.itemCount > 0) View.VISIBLE else View.GONE
        binding.swipeRefresh?.isRefreshing = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sync -> {
                viewModel.performFullSync()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}