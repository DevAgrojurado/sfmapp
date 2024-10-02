package com.agrojurado.sfmappv2.presentation.ui.home.fragmenthome

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.agrojurado.sfmappv2.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    private val _items = MutableLiveData<List<HomeItem>>()
    val items: LiveData<List<HomeItem>> = _items
    data class HomeItem(val title: String, val description: String, val imageResId: Int)

    init {
        loadItems()
    }

    private fun loadItems() {
        val sampleItems = listOf(
            HomeItem("Evaluacion Polinización", "Agregar una evaluación", R.drawable.evaluacion_img),
            //HomeItem("Null", "Null", R.drawable.agro_jurado),
            //HomeItem("Evaluación 3", "Descripción de la evaluación 3", R.drawable.agro_jurado)
        )
        _items.value = sampleItems
    }
}
