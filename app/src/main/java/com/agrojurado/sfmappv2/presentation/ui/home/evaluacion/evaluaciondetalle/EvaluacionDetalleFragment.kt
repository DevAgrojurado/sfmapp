package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluaciondetalle

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.databinding.FragmentListaEvaluacionBinding
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluacionfragmentsform.EvaluacionActivity
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluacionfragmentsform.EvaluacionViewModel
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.dialogdetail.EvaluacionDetalleDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EvaluacionDetalleFragment : Fragment() {
    private var _binding: FragmentListaEvaluacionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EvaluacionViewModel by activityViewModels()
    private lateinit var adapter: EvaluacionAdapter
    private val args: EvaluacionDetalleFragmentArgs by navArgs()

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
        observeViewModel()
        observeConnectivityState()
        observeLoadingState()
        setupListeners()
    }

    private fun observeLoadingState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.loadingIndicator?.visibility = if (isLoading) View.VISIBLE else View.GONE
                if (!isLoading && adapter.itemCount > 0) {
                    binding.rvEvaluacion.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvEvaluacion.layoutManager = LinearLayoutManager(requireContext())
        adapter = EvaluacionAdapter(
            onItemClick = { evaluacion, nombrePolinizador ->
                showEvaluacionDetalle(evaluacion, nombrePolinizador)
            },
            onEvaluacionAction = { evaluacion, action ->
                handleEvaluacionAction(evaluacion, action)
            }
        )
        binding.rvEvaluacion.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvaluacion.adapter = adapter
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
    }

    private fun handleEvaluacionAction(evaluacion: EvaluacionPolinizacion, action: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                when (action) {
                    "delete" -> viewModel.deleteEvaluacion(evaluacion)
                }
            } catch (e: Exception) {
                Log.e("EvaluacionDetalleFragment", "Error en acción: ${e.message}")
                showError("Error al realizar la acción")
            }
        }
    }

    private fun observeViewModel() {
        viewModel.evaluacionesPorSemana.observe(viewLifecycleOwner) { evaluacionesPorSemana ->
            try {
                val evaluacionesSemana = evaluacionesPorSemana[args.semana] ?: emptyList()
                val evaluacionesPolinizador = evaluacionesSemana.filter {
                    it.idPolinizador == args.idPolinizador
                }
                adapter.submitList(evaluacionesPolinizador)
                hideLoadingIndicators()
            } catch (e: Exception) {
                Log.e("EvaluacionDetalleFragment", "Error al observar evaluaciones: ${e.message}")
                showError("Error al cargar evaluaciones")
                hideLoadingIndicators()
            }
        }

        viewModel.operarioMap.observe(viewLifecycleOwner) { operarioMap ->
            try {
                adapter.setOperarioMap(operarioMap)
            } catch (e: Exception) {
                Log.e("EvaluacionDetalleFragment", "Error al observar operarios: ${e.message}")
                showError("Error al cargar operarios")
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

        viewModel.loadEvaluacionesPorSemana()
    }

    private fun hideLoadingIndicators() {
        binding.loadingIndicator?.visibility = View.GONE
        binding.rvEvaluacion.visibility = View.VISIBLE
        binding.swipeRefresh?.isRefreshing = false
    }


    private fun filterEvaluaciones(query: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val filteredList = viewModel.evaluacionesPorSemana.value?.get(args.semana)
                    ?.filter { evaluacion ->
                        evaluacion.idPolinizador == args.idPolinizador &&
                                (evaluacion.fecha?.contains(query, ignoreCase = true) == true ||
                                        evaluacion.hora?.contains(query, ignoreCase = true) == true ||
                                        evaluacion.observaciones?.contains(query, ignoreCase = true) == true)
                    } ?: emptyList()

                adapter.submitList(filteredList)
            } catch (e: Exception) {
                Log.e("EvaluacionDetalleFragment", "Error al filtrar: ${e.message}")
                showError("Error al filtrar evaluaciones")
            }
        }
    }

    private fun showEvaluacionDetalle(evaluacion: EvaluacionPolinizacion, nombrePolinizador: String) {
        try {
            val nombreEvaluador = viewModel.evaluador.value?.get(evaluacion.idEvaluador) ?: "Desconocido"
            val descripcionLote = viewModel.loteMap.value?.get(evaluacion.idlote) ?: "Desconocido"

            EvaluacionDetalleDialog(
                evaluacion,
                nombrePolinizador,
                nombreEvaluador,
                descripcionLote
            ).show(childFragmentManager, "EvaluacionDetalleDialog")
        } catch (e: Exception) {
            Log.e("EvaluacionDetalleFragment", "Error al mostrar diálogo: ${e.message}")
            showError("Error al mostrar detalle de evaluación")
        }
    }

    private fun observeConnectivityState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isOnline.collectLatest { isOnline ->
                viewModel.loadEvaluacionesPorSemana()
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_search, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterEvaluaciones(newText ?: "")
                return true
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}