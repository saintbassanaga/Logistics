package tech.bytesmind.logistics.shared.security.service;


import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import tech.bytesmind.logistics.shared.exceptions.SecurityViolationException;
import tech.bytesmind.logistics.shared.security.model.ActorType;
import tech.bytesmind.logistics.shared.security.model.SecurityContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service permettant d'extraire et de fournir le contexte de sécurité courant
 * basé sur le JSON Web Token (JWT) présent dans le contexte de sécurité Spring.
 *
 * Cette classe est utilisée pour garantir que les informations d'authentification
 * ainsi que les permissions de l'utilisateur courant peuvent être extraites
 * en toute sécurité et sous une forme normalisée.
 *
 * Responsabilités principales :
 * - Extraction et interprétation des claims présents dans le JWT.
 * - Validation des claims essentiels tels que 'sub', 'actor_type', et 'roles'.
 * - Gestion des rôles et des associations spécifiques, comme les agences.
 *
 * Principales exceptions :
 * - SecurityViolationException : levée en cas d'absence ou d'imprécision dans
 *   les claims exigés par la logique applicative.
 *
 * Contexte d'utilisation :
 * - Extraction du contexte de sécurité enrichi pour des besoins métiers.
 * - Interprétation des rôles et des relations d'appartenance pour les agences.
 */
@Service
public class SecurityContextService {
    
    private static final String CLAIM_ACTOR_TYPE = "actor_type";
    private static final String CLAIM_AGENCY_ID = "agency_id";
    private static final String CLAIM_ROLES = "roles";

    public SecurityContext getCurrentSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new SecurityViolationException("No valid JWT found in security context");
        }
        
        return extractSecurityContext(jwt);
    }
    

    private SecurityContext extractSecurityContext(Jwt jwt) {
        UUID userId = extractUserId(jwt);
        ActorType actorType = extractActorType(jwt);
        UUID agencyId = extractAgencyId(jwt, actorType);
        Set<String> roles = extractRoles(jwt);
        
        return new SecurityContext(userId, actorType, agencyId, roles);
    }
    
    private UUID extractUserId(Jwt jwt) {
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new SecurityViolationException("JWT missing 'sub' claim");
        }
        try {
            return UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            throw new SecurityViolationException("Invalid 'sub' claim format: " + sub);
        }
    }
    
    private ActorType extractActorType(Jwt jwt) {
        String actorTypeStr = jwt.getClaimAsString(CLAIM_ACTOR_TYPE);
        if (actorTypeStr == null) {
            throw new SecurityViolationException("JWT missing 'actor_type' claim");
        }
        try {
            return ActorType.valueOf(actorTypeStr);
        } catch (IllegalArgumentException e) {
            throw new SecurityViolationException("Invalid 'actor_type' claim: " + actorTypeStr);
        }
    }
    
    private UUID extractAgencyId(Jwt jwt, ActorType actorType) {
        String agencyIdStr = jwt.getClaimAsString(CLAIM_AGENCY_ID);
        
        if (actorType == ActorType.AGENCY_EMPLOYEE) {
            if (agencyIdStr == null) {
                throw new SecurityViolationException("AGENCY_EMPLOYEE must have 'agency_id' claim");
            }
            try {
                return UUID.fromString(agencyIdStr);
            } catch (IllegalArgumentException e) {
                throw new SecurityViolationException("Invalid 'agency_id' claim format");
            }
        }
        
        if (agencyIdStr != null) {
            throw new SecurityViolationException(
                actorType + " should not have 'agency_id' claim"
            );
        }
        
        return null;
    }
    
    private Set<String> extractRoles(Jwt jwt) {
        List<String> rolesList = jwt.getClaimAsStringList(CLAIM_ROLES);
        return rolesList != null ? new HashSet<>(rolesList) : Set.of();
    }
}