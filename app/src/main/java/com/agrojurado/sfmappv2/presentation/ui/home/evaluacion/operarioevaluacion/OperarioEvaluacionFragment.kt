package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.operarioevaluacion

import android.os.Bundle
import android.util.Log
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
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.databinding.FragmentListaEvaluacionBinding
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluaciongeneral.EvaluacionGeneralViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OperarioEvaluacionFragment : Fragment() {
    private var _binding: FragmentListaEvaluacionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EvaluacionGeneralViewModel by viewModels()
    private val args: OperarioEvaluacionFragmentArgs by navArgs()
    private lateinit var adapter: OperarioEvaluacionAdapter

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
        Log.d("OperarioEvaluacionFragment", "Semana recibida: ${args.semana}")
        setupRecyclerView()
        setupObservers()
        setupListeners()
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = OperarioEvaluacionAdapter(
            semana = args.semana,
            onItemClick = { idPolinizador, nombrePolinizador, idEvaluacionGeneral ->
                val currentDestination = findNavController().currentDestination?.id
                if (currentDestination != R.id.evaluacionDetalleFragment) {
                    findNavController().navigate(
                        OperarioEvaluacionFragmentDirections.actionOperarioEvaluacionToEvaluacionDetalle(
                            semana = args.semana,
                            idPolinizador = idPolinizador,
                            nombrePolinizador = nombrePolinizador,
                            idEvaluacionGeneral = idEvaluacionGeneral ?: -1
                        )
                    )
                } else {
                    Log.d("OperarioEvaluacionFragment", "Ya estás en EvaluacionDetalleFragment")
                }
            },
            countUniquePalms = { evaluaciones, evaluacionGeneralId ->
                // Incluimos evaluacionGeneralId en el filtro
                evaluaciones
                    .filter { it.palma != null && it.evaluacionGeneralId == evaluacionGeneralId }
                    .map { Pair(it.idlote, it.palma) }
                    .distinct()
                    .count()
            },
            getEvaluadorMap = { viewModel.evaluadorMap.value ?: emptyMap() },
            getLoteMap = { viewModel.loteMap.value ?: emptyMap() },
            getPhotoUrl = { semana, polinizadorId, evaluacionGeneralId ->
                viewModel.getPhotoUrlForPolinizador(semana, polinizadorId, evaluacionGeneralId)
            }
        )
        binding.rvEvaluacion.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@OperarioEvaluacionFragment.adapter
            visibility = View.GONE
        }
        binding.loadingIndicator?.visibility = View.VISIBLE
    }

    private fun setupListeners() {
        binding.evAddingBtn.setOnClickListener {
            findNavController().navigate(R.id.action_operarioEvaluacionFragment_to_evaluacionGeneralFragment)
        }
        binding.swipeRefresh?.setOnRefreshListener {
            viewModel.clearCache()
            loadData()
            binding.swipeRefresh?.isRefreshing = false
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("OperarioEvaluacionFragment", "isLoading: $isLoading")
            binding.loadingIndicator?.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.rvEvaluacion.visibility = if (!isLoading && adapter.itemCount > 0) View.VISIBLE else View.GONE
        }

        viewModel.evaluacionesGeneralesPorSemana.observe(viewLifecycleOwner) { evaluaciones ->
            Log.d("OperarioEvaluacionFragment", "Evaluaciones recibidas: ${evaluaciones?.size ?: 0}")
            if (evaluaciones != null) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val evaluacionesPorPolinizador = viewModel.getEvaluacionesPorPolinizador(args.semana)
                    Log.d("OperarioEvaluacionFragment", "Evaluaciones por polinizador: ${evaluacionesPorPolinizador.size}")
                    adapter.updateItems(evaluacionesPorPolinizador)
                    updateUI(evaluacionesPorPolinizador.isNotEmpty())
                }
            } else {
                Log.d("OperarioEvaluacionFragment", "Evaluaciones es null")
                updateUI(false)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    private fun updateUI(hasData: Boolean) {
        _binding?.let { binding ->
            binding.loadingIndicator?.visibility = View.GONE
            binding.rvEvaluacion.visibility = if (hasData) View.VISIBLE else View.GONE
            if (!hasData) {
                Toast.makeText(requireContext(), "No hay datos para la semana ${args.semana}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadData() {
        binding.loadingIndicator?.visibility = View.VISIBLE
        viewModel.loadEvaluacionesGeneralesPorSemana()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.evaluacionesGeneralesPorSemana.value?.isEmpty() != false) {
            loadData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}