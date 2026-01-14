package tech.bytesmind.logistics.auth.api.dto;

import tech.bytesmind.logistics.auth.domain.model.RoleScope;

import java.time.Instant;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        String code,
        String name,
        String description,
        RoleScope scope,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
