package com.agrojurado.sfmappv2.presentation.ui.home.fragmenthome

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.databinding.FragmentHomeBinding
import com.agrojurado.sfmappv2.presentation.ui.home.maps.MapsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: HomeAdapter

    // Variable para almacenar el último estado de conexión
    private var lastState: Boolean? = null

    // Variable para evitar mostrar "Modo sin conexión" en el primer inicio
    private var isFirstCheck = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        observeNetworkState()
    }

    private fun observeNetworkState() {
        lifecycleScope.launch {
            viewModel.isOnline.collect { isOnline ->
                // Solo muestra el Toast si el estado ha cambiado y no es la primera comprobación
                if (isFirstCheck) {
                    // En el primer chequeo, solo actualiza el estado anterior
                    lastState = isOnline
                    isFirstCheck = false
                } else {
                    // Muestra el Toast solo si el estado ha cambiado
                    if (lastState == null || lastState != isOnline) {
                        val message = if (isOnline) "Conectado" else "Modo sin conexión"
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        lastState = isOnline // Actualiza el estado anterior
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = HomeAdapter { item ->
            // Manejar el clic en el ítem aquí
            when (item.title) {
                "Evaluacion Polinización" -> {
                    findNavController().navigate(R.id.action_nav_home_to_listaEvaluacionFragment)
                }
                "Mapas" -> {
                    val intent = Intent(requireContext(), MapsActivity::class.java)
                    startActivity(intent)
                }
            }
        }
        binding.rvHome.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHome.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.items.observe(viewLifecycleOwner) { items ->
            adapter.updateItems(items)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
