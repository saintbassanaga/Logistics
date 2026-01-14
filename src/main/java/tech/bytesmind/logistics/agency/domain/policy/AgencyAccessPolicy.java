package tech.bytesmind.logistics.agency.domain.policy;


import org.springframework.stereotype.Component;
import tech.bytesmind.logistics.agency.domain.model.Agency;
import tech.bytesmind.logistics.shared.exceptions.SecurityViolationException;
import tech.bytesmind.logistics.shared.security.model.ActorType;
import tech.bytesmind.logistics.shared.security.model.SecurityContext;

import java.util.UUID;


@Component
public class AgencyAccessPolicy {

    /**
     * Vérifie si l'acteur peut accéder à une agence.
     */
    public boolean canAccess(SecurityContext context, UUID agencyId) {
        // Platform Admin : accès à toutes les agences
        if (context.actorType() == ActorType.PLATFORM_ADMIN) {
            return true;
        }

        // Agency Employee : accès uniquement à son agence
        if (context.actorType() == ActorType.AGENCY_EMPLOYEE) {
            if (context.agencyId() == null) {
                return false;
            }
            return context.agencyId().equals(agencyId);
        }

        // Customer : pas d'accès direct aux agences
        return false;
    }

    /**
     * Vérifie si l'acteur peut créer une agence.
     */
    public boolean canCreate(SecurityContext context) {
        // Seul Platform Admin peut créer des agences
        return context.actorType() == ActorType.PLATFORM_ADMIN;
    }

    /**
     * Vérifie si l'acteur peut modifier une agence.
     */
    public boolean canModify(SecurityContext context, Agency agency) {
        // Platform Admin : modification complète
        if (context.actorType() == ActorType.PLATFORM_ADMIN) {
            return true;
        }

        // Agency Employee avec rôle AGENCY_ADMIN : modifications limitées
        if (context.actorType() == ActorType.AGENCY_EMPLOYEE) {
            if (context.agencyId() == null || !context.agencyId().equals(agency.getId())) {
                return false;
            }
            return context.roles().contains("AGENCY_ADMIN");
        }

        return false;
    }

    /**
     * Vérifie si l'acteur peut suspendre une agence.
     */
    public boolean canSuspend(SecurityContext context) {
        // Seul Platform Admin peut suspendre
        return context.actorType() == ActorType.PLATFORM_ADMIN;
    }

    /**
     * Valide l'accès ou lance une exception.
     */
    public void validateAccess(SecurityContext context, UUID agencyId) {
        if (!canAccess(context, agencyId)) {
            throw new SecurityViolationException(
                    "Access denied to agency " + agencyId + " for actor " + context.userId()
            );
        }
    }

    /**
     * Valide la permission de modification ou lance une exception.
     */
    public void validateModify(SecurityContext context, Agency agency) {
        if (!canModify(context, agency)) {
            throw new SecurityViolationException(
                    "Modification denied for agency " + agency.getId()
            );
        }
    }
}
