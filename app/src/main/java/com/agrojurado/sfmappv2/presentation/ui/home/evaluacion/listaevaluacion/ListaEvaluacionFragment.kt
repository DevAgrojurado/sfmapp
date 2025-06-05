package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.listaevaluacion

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.databinding.FragmentListaEvaluacionBinding
import com.agrojurado.sfmappv2.domain.model.EvaluacionGeneral
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluaciongeneral.EvaluacionGeneralViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ListaEvaluacionFragment : Fragment() {
    private var _binding: FragmentListaEvaluacionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EvaluacionGeneralViewModel by viewModels()
    private lateinit var semanaAdapter: ListaEvaluacionAdapter
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
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        setupInitialViewState()

        if (isInitialLoad) {
            viewModel.loadEvaluacionesGeneralesPorSemana()
            isInitialLoad = false
        }
    }

    private fun setupInitialViewState() {
        binding.apply {
            rvEvaluacion.visibility = View.GONE
            loadingIndicator?.visibility = View.VISIBLE
        }
    }

    private fun setupRecyclerView() {
        semanaAdapter = ListaEvaluacionAdapter(
            emptyList(),
            onItemClick = { semana ->
                findNavController().navigate(
                    ListaEvaluacionFragmentDirections.actionListaEvaluacionToOperarioEvaluacionFragment(semana)
                )
            }
        )
        binding.rvEvaluacion.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvaluacion.adapter = semanaAdapter
    }

    private fun setupListeners() {
        binding.evAddingBtn.setOnClickListener {
            findNavController().navigate(R.id.action_listaEvaluacion_to_evaluacionGeneralFragment)
        }

        binding.swipeRefresh?.setOnRefreshListener {
            viewModel.clearCache() // Limpiar caché antes de recargar
            viewModel.loadEvaluacionesGeneralesPorSemana()
            binding.swipeRefresh?.isRefreshing = false
        }
    }

    private fun observeViewModel() {
        viewModel.evaluacionesGeneralesPorSemana.observe(viewLifecycleOwner) { evaluacionesPorSemana ->
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

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (!isLoading) hideLoadingIndicators()
        }
    }

    private fun updateEvaluacionesDisplay(evaluacionesPorSemana: Map<Int, List<EvaluacionGeneral>>) {
        try {
            val semanas = evaluacionesPorSemana.keys.toList().sortedDescending() // Changed to sortedDescending()
            if (semanas.isEmpty()) {
                binding.rvEvaluacion.visibility = View.GONE
                showNoRecordsMessage()
                return
            }
            hideNoRecordsMessage()
            semanaAdapter.updateItems(semanas)
            binding.rvEvaluacion.visibility = View.VISIBLE
            hideLoadingIndicators()
        } catch (e: Exception) {
            Log.e("ListaEvaluacionFragment", "Error updating display: ${e.message}")
            Toast.makeText(requireContext(), "Error al actualizar la lista", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showNoRecordsMessage() {
        val noRecordsTextView = TextView(requireContext()).apply {
            text = "No existen registros"
            textSize = 18f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            gravity = Gravity.CENTER
            alpha = 0.5f
            tag = "no_records_message"
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }
        (binding.root as ViewGroup).addView(noRecordsTextView)
    }

    private fun hideNoRecordsMessage() {
        binding.root.findViewWithTag<View>("no_records_message")?.let {
            (binding.root as ViewGroup).removeView(it)
        }
    }

    private fun hideLoadingIndicators() {
        binding.loadingIndicator?.visibility = View.GONE
        binding.rvEvaluacion.visibility = if (semanaAdapter.itemCount > 0) View.VISIBLE else View.GONE
        binding.swipeRefresh?.isRefreshing = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}