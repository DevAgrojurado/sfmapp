package com.agrojurado.sfmappv2.domain.security

import com.agrojurado.sfmappv2.domain.model.Lote
import com.agrojurado.sfmappv2.domain.model.Operario
import com.agrojurado.sfmappv2.domain.model.Usuario

interface RoleAccessControl {
    // Check if a user has a specific role
    fun hasRole(user: Usuario, role: String): Boolean

    // Role-based permission checks
    fun canCreateEvaluation(user: Usuario): Boolean
    fun canViewEvaluations(user: Usuario): Boolean
    fun canDeleteEvaluations(user: Usuario): Boolean
    fun canAccessAdminPanel(user: Usuario): Boolean

    // Finca-specific access
    fun canViewFinca(user: Usuario, fincaId: Int): Boolean

    fun filterLotsForUser(user: Usuario, allLots: List<Lote>): List<Lote>
    fun filterOperariosForUser(user: Usuario, allOperarios: List<Operario>): List<Operario>
}
