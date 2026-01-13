package tech.bytesmind.logistics.shared.security.model;

/**
 * Types d'acteurs dans la plateforme.
 * Définit la portée des permissions.
 */
public enum ActorType {
    /**
     * Employé d'une agence.
     * Possède un agency_id dans le JWT.
     */
    AGENCY_EMPLOYEE,
    
    /**
     * Client final.
     * Pas d'agency_id.
     */
    CUSTOMER,
    
    /**
     * Administrateur plateforme.
     * Pas d'agency_id, accès cross-tenant.
     */
    PLATFORM_ADMIN
}