package tech.bytesmind.logistics.shared.security.service;

import java.util.UUID;

/**
 * Service interface for promoting users across bounded contexts.
 * Used when a user creates an agency and needs to be promoted to AGENCY_ADMIN.
 * <p>
 * This interface lives in shared to allow the agency domain to use it
 * while the implementation lives in the auth domain.
 */
public interface UserPromotionService {

    /**
     * Promotes a CUSTOMER user to AGENCY_ADMIN of a new agency.
     * <p>
     * This operation:
     * - Changes the user's actorType from CUSTOMER to AGENCY_EMPLOYEE
     * - Sets the user's agencyId to the new agency
     * - Assigns the AGENCY_ADMIN role to the user
     *
     * @param userId   The ID of the user to promote
     * @param agencyId The ID of the newly created agency
     * @throws BusinessException if user is not found, not a CUSTOMER, or already has an agency
     */
    void promoteToAgencyAdmin(UUID userId, UUID agencyId);

    /**
     * Validates that a user can create and become admin of a new agency.
     *
     * @param userId The ID of the user to validate
     * @return true if user can create an agency, false otherwise
     */
    boolean canCreateAgency(UUID userId);
}