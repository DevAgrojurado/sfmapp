package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluaciondetalle

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
import coil.load
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.databinding.FragmentEvaluacionDetalleGeneralBinding
import com.agrojurado.sfmappv2.domain.model.EvaluacionGeneral
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.EvaluacionTableDialog
import com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluaciongeneral.EvaluacionGeneralViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EvaluacionDetalleFragment : Fragment() {
    private var _binding: FragmentEvaluacionDetalleGeneralBinding? = null
    private val binding get() = _binding!!
    private val evaluacionGeneralViewModel: EvaluacionGeneralViewModel by activityViewModels()
    private lateinit var adapter: EvaluacionGeneralAdapter
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
        _binding = FragmentEvaluacionDetalleGeneralBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        setupInitialViewState()
        setupEventClickListener()
    }

    private fun setupInitialViewState() {
        binding.apply {
            tvNoEvaluaciones.visibility = View.VISIBLE
            ivFoto.visibility = View.GONE
            ivSignature.visibility = View.GONE
            tvTapToViewEvents.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {
        adapter = EvaluacionGeneralAdapter(
            onItemClick = { evaluacionGeneral ->
                showEvaluacionGeneralDetail(evaluacionGeneral)
            }
        )
    }

    private fun setupObservers() {
        evaluacionGeneralViewModel.evaluacionesGeneralesPorSemana.observe(viewLifecycleOwner) { evaluacionesPorSemana ->
            val evaluacionesSemana = evaluacionesPorSemana[args.semana] ?: emptyList()
            val filtered = if (args.idEvaluacionGeneral != -1) {
                evaluacionesSemana.filter { it.id == args.idEvaluacionGeneral }
            } else {
                evaluacionesSemana.filter { it.idpolinizadorev == args.idPolinizador }
            }
            adapter.submitList(filtered)
            updateUI(filtered)
        }

        evaluacionGeneralViewModel.operarioMap.observe(viewLifecycleOwner) { operarioMap ->
            adapter.setOperarioMap(operarioMap)
            if (args.idEvaluacionGeneral == -1) {
                binding.tvPolinizador.text = operarioMap[args.idPolinizador] ?: "Desconocido"
            }
        }

        evaluacionGeneralViewModel.loteMap.observe(viewLifecycleOwner) { loteMap ->
            adapter.setLoteMap(loteMap)
        }

        evaluacionGeneralViewModel.loadEvaluacionesGeneralesPorSemana()
    }

    private fun setupEventClickListener() {
        binding.tvTapToViewEvents.setOnClickListener {
            val evaluacion = adapter.currentList.firstOrNull()
            if (evaluacion != null) {
                showEventsTablePopup(evaluacion)
            } else {
                Toast.makeText(requireContext(), "No hay evaluación seleccionada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEventsTablePopup(evaluacionGeneral: EvaluacionGeneral) {
        viewLifecycleOwner.lifecycleScope.launch {
            val evaluacionesPolinizacion = evaluacionGeneralViewModel.getEvaluacionesPorPolinizador(args.semana)
            val polinizadorEvents = evaluacionesPolinizacion[Pair(evaluacionGeneral.idpolinizadorev ?: 0,
                evaluacionGeneralViewModel.operarioMap.value?.get(evaluacionGeneral.idpolinizadorev ?: 0) ?: "Desconocido")]
                ?.filter { it.evaluacionGeneralId == evaluacionGeneral.id } ?: emptyList()

            if (polinizadorEvents.isNotEmpty()) {
                EvaluacionTableDialog.showEvaluationTableDialog(
                    context = requireContext(),
                    evaluaciones = polinizadorEvents,
                    evaluadorMap = evaluacionGeneralViewModel.evaluadorMap.value ?: emptyMap(),
                    loteMap = evaluacionGeneralViewModel.loteMap.value ?: emptyMap()
                )
            } else {
                Toast.makeText(requireContext(), "No hay eventos asociados a esta evaluación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(evaluaciones: List<EvaluacionGeneral>) {
        binding.apply {
            tvTotalEvaluaciones.text = "Total: ${evaluaciones.size} registros"
            tvNoEvaluaciones.visibility = if (evaluaciones.isEmpty()) View.VISIBLE else View.GONE
            tvTapToViewEvents.visibility = if (evaluaciones.isEmpty()) View.GONE else View.VISIBLE

            if (evaluaciones.isNotEmpty()) {
                val evaluacion = evaluaciones.first()
                tvPolinizador.text = evaluacionGeneralViewModel.operarioMap.value?.get(evaluacion.idpolinizadorev ?: 0) ?: "Desconocido"
                tvLote.text = evaluacionGeneralViewModel.loteMap.value?.get(evaluacion.idLoteev ?: 0) ?: "Desconocido"
                tvSeccion.text = "Semana: ${evaluacion.semana}"

                evaluacion.fotoPath?.let { path ->
                    ivFoto.load(path) {
                        placeholder(R.drawable.ic_more)
                        error(R.drawable.baseline_error_24)
                    }
                    ivFoto.visibility = View.VISIBLE
                } ?: run {
                    ivFoto.visibility = View.GONE
                }

                evaluacion.firmaPath?.let { path ->
                    ivSignature.load(path) {
                        placeholder(R.drawable.ic_more)
                        error(R.drawable.baseline_error_24)
                    }
                    ivSignature.visibility = View.VISIBLE
                } ?: run {
                    ivSignature.visibility = View.GONE
                }
            } else {
                ivFoto.visibility = View.GONE
                ivSignature.visibility = View.GONE
            }
        }
    }

    private fun showEvaluacionGeneralDetail(evaluacionGeneral: EvaluacionGeneral) {
        Toast.makeText(requireContext(), "Fecha: ${evaluacionGeneral.fecha}", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_search, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterEvaluaciones(newText ?: "")
                return true
            }
        })
    }

    private fun filterEvaluaciones(query: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val evaluacionesPorSemana = evaluacionGeneralViewModel.evaluacionesGeneralesPorSemana.value
            val evaluacionesSemana = evaluacionesPorSemana?.get(args.semana) ?: emptyList()
            val filtered = if (args.idEvaluacionGeneral != -1) {
                evaluacionesSemana.filter { it.id == args.idEvaluacionGeneral && matchesQuery(it, query) }
            } else {
                evaluacionesSemana.filter { it.idpolinizadorev == args.idPolinizador && matchesQuery(it, query) }
            }
            adapter.submitList(filtered)
            updateUI(filtered)
        }
    }

    private fun matchesQuery(evaluacion: EvaluacionGeneral, query: String): Boolean {
        return evaluacion.fecha?.contains(query, true) == true ||
                evaluacion.hora?.contains(query, true) == true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}