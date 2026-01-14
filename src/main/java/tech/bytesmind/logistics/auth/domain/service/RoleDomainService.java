package tech.bytesmind.logistics.auth.domain.service;

import org.springframework.stereotype.Service;
import tech.bytesmind.logistics.auth.domain.model.Role;
import tech.bytesmind.logistics.shared.exceptions.BusinessException;

/**
 * Service de domaine pour la logique métier des Roles.
 */
@Service
public class RoleDomainService {

    /**
     * Valide qu'un Role respecte les invariants métier.
     */
    public void validateRole(Role role) {
        if (role.getCode() == null || role.getCode().isBlank()) {
            throw new BusinessException("Role code is required");
        }

        if (role.getName() == null || role.getName().isBlank()) {
            throw new BusinessException("Role name is required");
        }

        if (role.getScope() == null) {
            throw new BusinessException("Role scope is required");
        }

        // Le code doit être en majuscules et sans espaces
        if (!role.getCode().matches("^[A-Z_]+$")) {
            throw new BusinessException(
                    "Role code must be uppercase letters and underscores only"
            );
        }
    }

    /**
     * Désactive un rôle.
     */
    public void deactivate(Role role) {
        if (!role.isActive()) {
            throw new BusinessException("Role is already inactive");
        }
        role.setActive(false);
    }

    /**
     * Réactive un rôle.
     */
    public void activate(Role role) {
        if (role.isActive()) {
            throw new BusinessException("Role is already active");
        }
        role.setActive(true);
    }
}