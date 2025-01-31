package com.agrojurado.sfmappv2.presentation.ui.home.fragmenthome

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.agrojurado.sfmappv2.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _items = MutableLiveData<List<HomeItem>>()
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline
    val items: LiveData<List<HomeItem>> = _items

    data class HomeItem(val title: String, val description: String, val imageResId: Int)

    init {
        loadItems()
        observeNetworkState()
    }

    private fun observeNetworkState() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    private fun loadItems() {
        val sampleItems = listOf(
            HomeItem("Evaluacion Polinización", "Agregar una evaluación", R.drawable.evaluacion_img),
            HomeItem("Mapas", "Ver mapas", R.drawable.agro_jurado),
            HomeItem("Talento Humano", "Agregar formulario", R.drawable.agro_jurado)
        )
        _items.value = sampleItems
    }
}
