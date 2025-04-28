package com.agrojurado.sfmappv2.presentation.ui.home.fragmenthome

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.agrojurado.sfmappv2.R
import com.agrojurado.sfmappv2.utils.NetworkMonitor
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
        try {
            // Usamos el NetworkMonitor singleton en lugar de registrar nuestro propio callback
            val networkMonitor = NetworkMonitor.getInstance(context)
            networkMonitor.observeForever { isConnected ->
                _isOnline.value = isConnected
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al observar el estado de red: ${e.message}", e)
            // En caso de error, asumimos que hay conexión
            _isOnline.value = true
        }
    }

    private fun loadItems() {
        val sampleItems = listOf(
            HomeItem("Evaluacion Polinización", "Agregar una evaluación", R.drawable.evaluacion_img),
            HomeItem("Mapas", "Ver mapas", R.drawable.agro_jurado),
            HomeItem("Talento Humano", "Agregar formulario", R.drawable.agro_jurado)
        )
        _items.value = sampleItems
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}
