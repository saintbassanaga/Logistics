package tech.bytesmind.logistics.agency.domain.policy;


import org.springframework.stereotype.Component;
import tech.bytesmind.logistics.agency.domain.model.AgencyLocation;
import tech.bytesmind.logistics.shared.exceptions.SecurityViolationException;
import tech.bytesmind.logistics.shared.security.model.ActorType;
import tech.bytesmind.logistics.shared.security.model.SecurityContext;

import java.util.UUID;

/**
 * Policy ABAC pour le contrôle d'accès aux localisations.
 */
@Component
public class LocationAccessPolicy {

    public boolean canAccess(SecurityContext context, UUID agencyId) {
        if (context.actorType() == ActorType.PLATFORM_ADMIN) {
            return true;
        }

        if (context.actorType() == ActorType.AGENCY_EMPLOYEE) {
            return context.agencyId() != null && context.agencyId().equals(agencyId);
        }

        return false;
    }

    public boolean canModify(SecurityContext context, AgencyLocation location) {
        if (context.actorType() == ActorType.PLATFORM_ADMIN) {
            return true;
        }

        if (context.actorType() == ActorType.AGENCY_EMPLOYEE) {
            if (context.agencyId() == null || !context.agencyId().equals(location.getAgencyId())) {
                return false;
            }
            return context.roles().contains("AGENCY_ADMIN") ||
                    context.roles().contains("LOCATION_MANAGER");
        }

        return false;
    }

    public void validateAccess(SecurityContext context, UUID agencyId) {
        if (!canAccess(context, agencyId)) {
            throw new SecurityViolationException(
                    "Access denied to locations of agency " + agencyId
            );
        }
    }
}
