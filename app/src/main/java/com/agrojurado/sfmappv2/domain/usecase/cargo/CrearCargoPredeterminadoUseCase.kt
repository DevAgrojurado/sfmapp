package com.agrojurado.sfmappv2.domain.usecase.cargo

import com.agrojurado.sfmappv2.domain.repository.CargoRepository
import javax.inject.Inject

class CrearCargoPredeterminadoUseCase @Inject constructor(private val repository: CargoRepository) {
    suspend operator fun invoke() {
        //repository.crearCargoPredeterminado()
    }
}