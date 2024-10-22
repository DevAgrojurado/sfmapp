package com.agrojurado.sfmappv2.presentation.ui.admin.lotes

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.agrojurado.sfmappv2.domain.model.Finca
import com.agrojurado.sfmappv2.domain.model.Lote
import com.agrojurado.sfmappv2.domain.repository.FincaRepository
import com.agrojurado.sfmappv2.domain.repository.LoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LotesViewModel @Inject constructor(
    private val loteRepository: LoteRepository,
    private val fincaRepository: FincaRepository
) : ViewModel() {

    val lotes = loteRepository.getAllLotes().asLiveData()
    val fincas: LiveData<List<Finca>> = fincaRepository.getAllFincas().asLiveData()

    fun insertLote(lote: Lote) = viewModelScope.launch {
        loteRepository.insertLote(lote)
    }

    fun updateLote(lote: Lote) = viewModelScope.launch {
        loteRepository.updateLote(lote)
    }

    fun deleteLote(lote: Lote) = viewModelScope.launch {
        loteRepository.deleteLote(lote)
    }

    fun deleteAllLotes() = viewModelScope.launch {
        loteRepository.deleteAllLotes()
    }
}