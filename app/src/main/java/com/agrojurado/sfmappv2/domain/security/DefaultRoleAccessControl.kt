package com.agrojurado.sfmappv2.domain.security

import com.agrojurado.sfmappv2.domain.model.Lote
import com.agrojurado.sfmappv2.domain.model.Operario
import com.agrojurado.sfmappv2.domain.model.Usuario

class DefaultRoleAccessControl : RoleAccessControl {
    // Verifica si el usuario tiene el rol especificado
    override fun hasRole(user: Usuario, role: String): Boolean {
        return user.rol.equals(role, ignoreCase = true)
    }

    // Evaluador puede crear evaluación solo si está asignado a una finca
    override fun canCreateEvaluation(user: Usuario): Boolean {
        return when {
            hasRole(user, UserRoleConstants.ROLE_ADMIN) -> true
            hasRole(user, UserRoleConstants.ROLE_COORDINATOR) -> true
            hasRole(user, UserRoleConstants.ROLE_EVALUATOR) -> {
                // El evaluador solo puede crear evaluaciones para su finca asignada
                user.idFinca != null // Asegúrate de que el evaluador tiene asignada una finca
            }
            else -> false
        }
    }

    override fun filterLotsForUser(user: Usuario, allLots: List<Lote>): List<Lote> {
        return when {
            hasRole(user, UserRoleConstants.ROLE_ADMIN) -> allLots
            hasRole(user, UserRoleConstants.ROLE_COORDINATOR) -> allLots
            hasRole(user, UserRoleConstants.ROLE_EVALUATOR) -> {
                // If user is an evaluator, only show lots from their assigned farm
                user.idFinca?.let { assignedFarmId ->
                    allLots.filter { it.idFinca == assignedFarmId }
                } ?: emptyList()
            }
            else -> emptyList()
        }
    }

    override fun filterOperariosForUser(user: Usuario, allOperarios: List<Operario>): List<Operario> {
        return when {
            hasRole(user, UserRoleConstants.ROLE_ADMIN) -> allOperarios // Admin ve todos
            hasRole(user, UserRoleConstants.ROLE_COORDINATOR) -> allOperarios.filter { it.activo } // Coordinador ve solo activos
            hasRole(user, UserRoleConstants.ROLE_EVALUATOR) -> {
                user.idFinca?.let { assignedFarmId ->
                    allOperarios.filter { it.fincaId == assignedFarmId && it.activo } // Evaluador ve solo activos de su finca
                } ?: emptyList()
            }
            else -> emptyList()
        }
    }


    // Evaluador puede ver evaluaciones solo de su finca asignada
    override fun canViewEvaluations(user: Usuario): Boolean {
        return when {
            hasRole(user, UserRoleConstants.ROLE_ADMIN) -> true
            hasRole(user, UserRoleConstants.ROLE_COORDINATOR) -> true
            hasRole(user, UserRoleConstants.ROLE_EVALUATOR) -> {
                // El evaluador solo puede ver evaluaciones de su finca asignada
                user.idFinca != null
            }
            else -> false
        }
    }

    // Administrador o coordinador pueden eliminar evaluaciones
    override fun canDeleteEvaluations(user: Usuario): Boolean {
        return when {
            hasRole(user, UserRoleConstants.ROLE_ADMIN) -> true
            hasRole(user, UserRoleConstants.ROLE_COORDINATOR) -> true
            else -> false
        }
    }

    // Los administradores pueden acceder al panel de administración
    override fun canAccessAdminPanel(user: Usuario): Boolean {
        return hasRole(user, UserRoleConstants.ROLE_ADMIN)
    }

    // Verificar si un evaluador tiene acceso a la finca especificada
    override fun canViewFinca(user: Usuario, fincaId: Int): Boolean {
        return when {
            hasRole(user, UserRoleConstants.ROLE_ADMIN) -> true
            hasRole(user, UserRoleConstants.ROLE_COORDINATOR) -> true
            hasRole(user, UserRoleConstants.ROLE_EVALUATOR) -> {
                // El evaluador solo puede ver la finca que tiene asignada
                user.idFinca == fincaId
            }
            else -> false
        }
    }


}
