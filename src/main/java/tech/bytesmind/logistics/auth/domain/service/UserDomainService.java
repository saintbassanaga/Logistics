package tech.bytesmind.logistics.auth.domain.service;

import org.springframework.stereotype.Service;
import tech.bytesmind.logistics.auth.domain.model.Role;
import tech.bytesmind.logistics.auth.domain.model.RoleScope;
import tech.bytesmind.logistics.auth.domain.model.User;
import tech.bytesmind.logistics.shared.exceptions.BusinessException;
import tech.bytesmind.logistics.shared.security.model.ActorType;

import java.util.UUID;

/**
 * Service de domaine pour la logique métier des Users.
 * Implémente les invariants critiques du contexte Auth.
 */
@Service
public class UserDomainService {

    /**
     * Valide qu'un User respecte les invariants métier.
     * Règles:
     * - AGENCY_EMPLOYEE doit avoir un agencyId
     * - PLATFORM_ADMIN ne doit PAS avoir d'agencyId
     * - CUSTOMER ne doit PAS avoir d'agencyId
     */
    public void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BusinessException("Email is required");
        }

        if (user.getFirstName() == null || user.getFirstName().isBlank()) {
            throw new BusinessException("First name is required");
        }

        if (user.getLastName() == null || user.getLastName().isBlank()) {
            throw new BusinessException("Last name is required");
        }

        if (user.getActorType() == null) {
            throw new BusinessException("Actor type is required");
        }

        validateAgencyIdConsistency(user);
    }

    /**
     * Valide la cohérence entre actorType et agencyId.
     */
    public void validateAgencyIdConsistency(User user) {
        if (user.getActorType() == ActorType.AGENCY_EMPLOYEE) {
            if (user.getAgencyId() == null) {
                throw new BusinessException("AGENCY_EMPLOYEE must have an agency_id");
            }
        } else {
            // PLATFORM_ADMIN et CUSTOMER ne doivent PAS avoir d'agencyId
            if (user.getAgencyId() != null) {
                throw new BusinessException(
                        user.getActorType() + " must NOT have an agency_id"
                );
            }
        }
    }

    /**
     * Désactive un utilisateur.
     */
    public void deactivate(User user) {
        if (!user.isActive()) {
            throw new BusinessException("User is already inactive");
        }
        user.setActive(false);
    }

    /**
     * Réactive un utilisateur.
     */
    public void activate(User user) {
        if (user.isActive()) {
            throw new BusinessException("User is already active");
        }
        user.setActive(true);
    }

    /**
     * Assigne un rôle à un utilisateur avec validation.
     */
    public void assignRole(User user, Role role) {
        // Vérifier que le scope du rôle correspond à l'actorType
        if (role.getScope() == RoleScope.AGENCY && user.getActorType() != ActorType.AGENCY_EMPLOYEE) {
            throw new BusinessException(
                    "AGENCY-scoped roles can only be assigned to AGENCY_EMPLOYEE"
            );
        }

        if (role.getScope() == RoleScope.PLATFORM && user.getActorType() != ActorType.PLATFORM_ADMIN) {
            throw new BusinessException(
                    "PLATFORM-scoped roles can only be assigned to PLATFORM_ADMIN"
            );
        }

        if (!role.isActive()) {
            throw new BusinessException("Cannot assign inactive role");
        }

        if (user.getRoles().contains(role)) {
            throw new BusinessException("User already has this role");
        }

        user.getRoles().add(role);
    }

    /**
     * Révoque un rôle d'un utilisateur.
     */
    public void revokeRole(User user, Role role) {
        if (!user.getRoles().contains(role)) {
            throw new BusinessException("User does not have this role");
        }

        user.getRoles().remove(role);
    }

    /**
     * Change l'agence d'un employé.
     * IMPORTANT: Entraîne l'invalidation du JWT actuel.
     */
    public void changeAgency(User user, UUID newAgencyId) {
        if (user.getActorType() != ActorType.AGENCY_EMPLOYEE) {
            throw new BusinessException("Only AGENCY_EMPLOYEE can change agency");
        }

        if (newAgencyId == null) {
            throw new BusinessException("New agency ID cannot be null");
        }

        if (user.getAgencyId() != null && user.getAgencyId().equals(newAgencyId)) {
            throw new BusinessException("User is already in this agency");
        }

        user.setAgencyId(newAgencyId);
        // Note: Les rôles actuels sont conservés mais devront peut-être être réévalués
    }

    /**
     * Vérifie si un utilisateur peut se connecter.
     */
    public boolean canLogin(User user) {
        return user.isActive() && user.isEmailVerified();
    }

    /**
     * Valide qu'un utilisateur peut se connecter ou lance une exception.
     */
    public void validateCanLogin(User user) {
        if (!user.isActive()) {
            throw new BusinessException("User account is inactive");
        }

        if (!user.isEmailVerified()) {
            throw new BusinessException("Email must be verified before login");
        }
    }
}