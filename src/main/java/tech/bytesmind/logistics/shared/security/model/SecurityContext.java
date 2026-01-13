package tech.bytesmind.logistics.shared.security.model;

import java.util.Set;
import java.util.UUID;

/**
 * Represents the security context of a user in the system.
 * This record encapsulates details related to the user's identity,
 * role, and contextual permissions, enabling secure handling of
 * multi-tenant and role-based access control.
 * <p>
 * Fields:
 * - `userId`: The unique identifier of the user.
 * - `actorType`: The type of the actor, specifying whether the user is
 *   a customer, an agency employee, or a platform administrator.
 * - `agencyId`: The identifier of the agency to which the user belongs.
 *   It is null for users of type CUSTOMER or PLATFORM_ADMIN.
 * - `roles`: A set of roles assigned to the user.
 * <p>
 * Responsibilities:
 * - Facilitates role-based permission checks.
 * - Determines whether the user belongs to a specific agency.
 * - Distinguishes a user type for access control and multi-tenancy validation.
 * <p>
 * Methods:
 * - `hasRole(String role)`: Checks if the user has a specific role.
 * - `isAgencyEmployee()`: Checks if the user is an employee of an agency.
 * - `isCustomer()`: Checks if the user is a customer.
 * - `isPlatformAdmin()`: Checks if the user is a platform administrator.
 * - `belongsToAgency(UUID agencyId)`: Validates whether the user belongs to
 *   a specified agency. This is critical for ensuring multi-tenancy boundaries.
 */
public record SecurityContext(
    UUID userId,
    ActorType actorType,
    UUID agencyId,
    Set<String> roles
) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
    
    public boolean isAgencyEmployee() {
        return actorType == ActorType.AGENCY_EMPLOYEE;
    }
    
    public boolean isCustomer() {
        return actorType == ActorType.CUSTOMER;
    }
    
    public boolean isPlatformAdmin() {
        return actorType == ActorType.PLATFORM_ADMIN;
    }

    public boolean belongsToAgency(UUID agencyId) {
        if (!isAgencyEmployee()) {
            return false;
        }
        return this.agencyId != null && this.agencyId.equals(agencyId);
    }
}