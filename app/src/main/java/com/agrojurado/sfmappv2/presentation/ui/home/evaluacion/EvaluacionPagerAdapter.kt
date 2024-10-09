package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class EvaluacionPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    private val fragments = mutableListOf<Fragment>()

    init {
        fragments.add(InformacionGeneralFragment())
        fragments.add(DetallesPolinizacionFragment())
        fragments.add(EvaluacionFragment())
    }

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]

    fun getFragment(position: Int): Fragment = fragments[position]
}