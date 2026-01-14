package tech.bytesmind.logistics.auth.domain.policy;

import org.springframework.stereotype.Component;
import tech.bytesmind.logistics.auth.domain.model.User;
import tech.bytesmind.logistics.shared.exceptions.SecurityViolationException;
import tech.bytesmind.logistics.shared.security.model.ActorType;
import tech.bytesmind.logistics.shared.security.model.SecurityContext;

import java.util.UUID;

/**
 * Politique d'accès pour les Users (ABAC).
 * Implémente les règles d'autorisation sur les ressources User.
 */
@Component
public class UserAccessPolicy {

    /**
     * Vérifie si l'acteur peut accéder à un utilisateur.
     */
    public boolean canAccess(SecurityContext context, User targetUser) {
        // Platform Admin : accès à tous les utilisateurs
        if (context.isPlatformAdmin()) {
            return true;
        }

        // Agency Employee : accès aux utilisateurs de son agence uniquement
        if (context.isAgencyEmployee()) {
            // Peut voir les autres employés de son agence
            if (targetUser.isAgencyEmployee() &&
                    context.agencyId() != null &&
                    context.agencyId().equals(targetUser.getAgencyId())) {
                return true;
            }
        }

        // Un utilisateur peut toujours accéder à son propre profil
        return context.userId().equals(targetUser.getId());
    }

    /**
     * Vérifie si l'acteur peut créer un utilisateur.
     */
    public boolean canCreate(SecurityContext context, ActorType targetActorType) {
        // Platform Admin : peut créer tous types d'utilisateurs
        if (context.isPlatformAdmin()) {
            return true;
        }

        // Agency Admin : peut créer des AGENCY_EMPLOYEE pour son agence
        if (context.isAgencyEmployee() && context.hasRole("AGENCY_ADMIN")) {
            return targetActorType == ActorType.AGENCY_EMPLOYEE;
        }

        return false;
    }

    /**
     * Vérifie si l'acteur peut modifier un utilisateur.
     */
    public boolean canModify(SecurityContext context, User targetUser) {
        // Platform Admin : modification complète
        if (context.isPlatformAdmin()) {
            return true;
        }

        // Agency Admin : peut modifier les employés de son agence
        if (context.isAgencyEmployee() && context.hasRole("AGENCY_ADMIN")) {
            if (targetUser.isAgencyEmployee() &&
                    context.agencyId() != null &&
                    context.agencyId().equals(targetUser.getAgencyId())) {
                return true;
            }
        }

        // Un utilisateur peut modifier son propre profil (limité)
        return context.userId().equals(targetUser.getId());
    }

    /**
     * Vérifie si l'acteur peut assigner des rôles à un utilisateur.
     */
    public boolean canAssignRoles(SecurityContext context, User targetUser) {
        // Platform Admin : peut assigner tous les rôles
        if (context.isPlatformAdmin()) {
            return true;
        }

        // Agency Admin : peut assigner des rôles aux employés de son agence
        if (context.isAgencyEmployee() && context.hasRole("AGENCY_ADMIN")) {
            if (targetUser.isAgencyEmployee() &&
                    context.agencyId() != null &&
                    context.agencyId().equals(targetUser.getAgencyId())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Vérifie si l'acteur peut désactiver un utilisateur.
     */
    public boolean canDeactivate(SecurityContext context, User targetUser) {
        // Platform Admin : peut désactiver n'importe qui
        if (context.isPlatformAdmin()) {
            return true;
        }

        // Agency Admin : peut désactiver les employés de son agence
        if (context.isAgencyEmployee() && context.hasRole("AGENCY_ADMIN")) {
            if (targetUser.isAgencyEmployee() &&
                    context.agencyId() != null &&
                    context.agencyId().equals(targetUser.getAgencyId())) {
                // Ne peut pas se désactiver soi-même
                return !context.userId().equals(targetUser.getId());
            }
        }

        return false;
    }

    /**
     * Vérifie si l'acteur peut changer l'agence d'un employé.
     */
    public boolean canChangeAgency(SecurityContext context, User targetUser) {
        // Seul Platform Admin peut changer l'agence d'un employé
        return context.isPlatformAdmin() && targetUser.isAgencyEmployee();
    }

    /**
     * Vérifie si l'acteur peut lister les utilisateurs d'une agence.
     */
    public boolean canListAgencyUsers(SecurityContext context, UUID agencyId) {
        // Platform Admin : peut lister tous
        if (context.isPlatformAdmin()) {
            return true;
        }

        // Agency Employee : peut lister les utilisateurs de son agence
        if (context.isAgencyEmployee()) {
            return context.agencyId() != null && context.agencyId().equals(agencyId);
        }

        return false;
    }

    /**
     * Valide l'accès ou lance une exception.
     */
    public void validateAccess(SecurityContext context, User targetUser) {
        if (!canAccess(context, targetUser)) {
            throw new SecurityViolationException(
                    "Access denied to user " + targetUser.getId()
            );
        }
    }

    /**
     * Valide la permission de modification ou lance une exception.
     */
    public void validateModify(SecurityContext context, User targetUser) {
        if (!canModify(context, targetUser)) {
            throw new SecurityViolationException(
                    "Modification denied for user " + targetUser.getId()
            );
        }
    }

    /**
     * Valide la permission d'assignation de rôles ou lance une exception.
     */
    public void validateAssignRoles(SecurityContext context, User targetUser) {
        if (!canAssignRoles(context, targetUser)) {
            throw new SecurityViolationException(
                    "Role assignment denied for user " + targetUser.getId()
            );
        }
    }
}