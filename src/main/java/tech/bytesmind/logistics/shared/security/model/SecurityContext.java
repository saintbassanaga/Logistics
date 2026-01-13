package tech.bytesmind.logistics.shared.security.model;

import java.util.Set;
import java.util.UUID;

/**
 * Security Context enrichi extrait du JWT.
 * SOURCE UNIQUE DE VÉRITÉ pour l'identité et les permissions.
 * 
 * Conforme à ADR-003 et ADR-004.
 */
public record SecurityContext(
    UUID userId,
    ActorType actorType,
    UUID agencyId, // null pour CUSTOMER et PLATFORM_ADMIN
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
    
    /**
     * Vérifie que cet acteur appartient à l'agence spécifiée.
     * CRITIQUE pour la multi-tenance.
     */
    public boolean belongsToAgency(UUID agencyId) {
        if (!isAgencyEmployee()) {
            return false;
        }
        return this.agencyId != null && this.agencyId.equals(agencyId);
    }
}