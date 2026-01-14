package tech.bytesmind.logistics.auth.api.dto;

import tech.bytesmind.logistics.shared.security.model.ActorType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        ActorType actorType,
        UUID agencyId,
        String jobTitle,
        String department,
        boolean active,
        boolean emailVerified,
        Set<String> roles,
        Instant createdAt,
        Instant updatedAt,
        Instant lastLoginAt
) {
}
