package com.agrojurado.sfmappv2.domain.model

enum class Role {
    ADMIN,
    SUPERVISOR,
    OPERADOR;

    fun canEdit(): Boolean = when (this) {
        ADMIN -> true
        SUPERVISOR -> false
        OPERADOR -> true // Solo para evaluaciones
    }

    fun canDelete(): Boolean = when (this) {
        ADMIN -> true
        else -> false
    }

    fun hasFullAccess(): Boolean = when (this) {
        ADMIN, SUPERVISOR -> true
        OPERADOR -> false
    }
}