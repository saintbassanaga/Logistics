package tech.bytesmind.logistics.auth.domain.policy;

import org.springframework.stereotype.Component;
import tech.bytesmind.logistics.auth.domain.model.Role;
import tech.bytesmind.logistics.auth.domain.model.RoleScope;
import tech.bytesmind.logistics.shared.exceptions.SecurityViolationException;
import tech.bytesmind.logistics.shared.security.model.SecurityContext;

/**
 * Politique d'accès pour les Roles (ABAC).
 */
@Component
public class RoleAccessPolicy {

    /**
     * Vérifie si l'acteur peut créer un rôle.
     */
    public boolean canCreate(SecurityContext context) {
        // Seul Platform Admin peut créer des rôles
        return context.isPlatformAdmin();
    }

    /**
     * Vérifie si l'acteur peut modifier un rôle.
     */
    public boolean canModify(SecurityContext context, Role role) {
        // Seul Platform Admin peut modifier des rôles
        return context.isPlatformAdmin();
    }

    /**
     * Vérifie si l'acteur peut assigner un rôle spécifique.
     */
    public boolean canAssign(SecurityContext context, Role role) {
        // Platform Admin : peut assigner tous les rôles
        if (context.isPlatformAdmin()) {
            return true;
        }

        // Agency Admin : peut assigner uniquement les rôles AGENCY-scoped
        if (context.isAgencyEmployee() && context.hasRole("AGENCY_ADMIN")) {
            return role.getScope() == RoleScope.AGENCY;
        }

        return false;
    }

    /**
     * Valide l'assignation d'un rôle ou lance une exception.
     */
    public void validateAssign(SecurityContext context, Role role) {
        if (!canAssign(context, role)) {
            throw new SecurityViolationException(
                    "Cannot assign role " + role.getCode()
            );
        }
    }
}