package com.agrojurado.sfmappv2.presentation.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.presentation.ui.admin.areas.AreasActivity
import com.agrojurado.sfmappv2.presentation.ui.admin.cargos.CargosActivity
import com.agrojurado.sfmappv2.presentation.ui.admin.fincas.FincasActivity
import com.agrojurado.sfmappv2.presentation.ui.admin.lotes.LotesActivity
import com.agrojurado.sfmappv2.presentation.ui.admin.operarios.OperariosActivity
import com.agrojurado.sfmappv2.presentation.ui.admin.usuarios.UsuariosActivity

class AdminFragment : Fragment() {

    private lateinit var viewModel: AdminViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(AdminViewModel::class.java)

        // Encuentra los TextViews
        val tvOperario: TextView = view.findViewById(R.id.tv_operario)
        val tvCargo: TextView = view.findViewById(R.id.tv_cargo)
        val tvUsuarios: TextView = view.findViewById(R.id.tv_usuarios)
        val tvAreas: TextView = view.findViewById(R.id.tv_areas)
        val tvFincas: TextView = view.findViewById(R.id.tv_fincas)
        val tvLotes: TextView = view.findViewById(R.id.tv_lotes)

        // Configura los OnClickListener
        tvCargo.setOnClickListener {
            val intent = Intent(activity, CargosActivity::class.java)
            startActivity(intent)
        }

        tvOperario.setOnClickListener {
            val intent = Intent(activity, OperariosActivity::class.java)
            startActivity(intent)
        }

        tvUsuarios.setOnClickListener {
            val intent = Intent(activity, UsuariosActivity::class.java)
            startActivity(intent)
        }

        tvAreas.setOnClickListener {
            val intent = Intent(activity, AreasActivity::class.java)
            startActivity(intent)
        }
        tvFincas.setOnClickListener {
            val intent = Intent(activity, FincasActivity::class.java)
            startActivity(intent)
        }
        tvLotes.setOnClickListener {
            val intent = Intent(activity, LotesActivity::class.java)
            startActivity(intent)
        }
    }
}