package tech.bytesmind.logistics.auth.api.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * DTO de réponse après inscription d'un utilisateur.
 */
public record UserRegistrationResponse(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        String phone,
        String actorType,
        Set<String> roles,
        boolean active,
        boolean emailVerified,
        Instant createdAt
) {
}