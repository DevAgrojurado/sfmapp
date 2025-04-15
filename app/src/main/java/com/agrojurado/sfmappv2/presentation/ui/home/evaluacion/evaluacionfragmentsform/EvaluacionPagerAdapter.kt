package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluacionfragmentsform

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.apache.poi.util.LittleEndian.putInt

class EvaluacionPagerAdapter(
    activity: FragmentActivity,
    private val evaluacionGeneralId: Int,
    private val operarioId: Int?,
    private val loteId: Int?,
    private val seccion: Int?
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> InformacionGeneralFragment().apply {
                arguments = Bundle().apply {
                    putInt("evaluacionGeneralId", evaluacionGeneralId)
                    putInt("operarioId", operarioId ?: -1)
                    putInt("loteId", loteId ?: -1)
                    putInt("seccion", seccion ?: -1)
                }
            }
            1 -> DetallesPolinizacionFragment().apply {
                arguments = Bundle().apply {
                    putInt("evaluacionGeneralId", evaluacionGeneralId)
                }
            }
            2 -> EvaluacionFragment().apply {
                arguments = Bundle().apply {
                    putInt("evaluacionGeneralId", evaluacionGeneralId)
                }
            }
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }

    fun getFragment(position: Int): Fragment {
        return createFragment(position)
    }
}