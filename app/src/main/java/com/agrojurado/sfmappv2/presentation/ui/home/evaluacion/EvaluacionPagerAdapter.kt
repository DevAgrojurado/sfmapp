package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class EvaluacionPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position + 1) {
            1 -> InformacionGeneralFragment()
            2 -> DetallesPolinizacionFragment()
            3 -> EvaluacionFragment()
            //4 -> ObservacionesFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}