package tech.bytesmind.logistics.auth.domain.model;

/**
 * Scope d'un rôle.
 */
public enum RoleScope {
    /**
     * Rôle scopé à une agence (ex: AGENCY_ADMIN, SHIPMENT_MANAGER).
     * Assigné aux AGENCY_EMPLOYEE.
     */
    AGENCY,

    /**
     * Rôle plateforme (ex: PLATFORM_ADMIN).
     * Assigné aux PLATFORM_ADMIN.
     */
    PLATFORM,

    /**
     * Rôle client (ex: USER).
     * Assigné aux CUSTOMER.
     */
    CUSTOMER
}