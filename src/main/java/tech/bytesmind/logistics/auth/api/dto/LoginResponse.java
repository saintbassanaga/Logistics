package tech.bytesmind.logistics.auth.api.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * DTO de réponse après authentification réussie.
 * Contient le token JWT et les informations utilisateur.
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        String username,
        String email,
        String firstName,
        String lastName,
        String actorType,
        UUID agencyId,
        Set<String> roles,
        Instant lastLoginAt
) {
    public LoginResponse(String accessToken, long expiresIn, UUID userId, String username,
                         String email, String firstName, String lastName, String actorType,
                         UUID agencyId, Set<String> roles, Instant lastLoginAt) {
        this(accessToken, "Bearer", expiresIn, userId, username, email, firstName, lastName,
                actorType, agencyId, roles, lastLoginAt);
    }
}