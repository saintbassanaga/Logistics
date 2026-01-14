package tech.bytesmind.logistics.auth.domain.event;

import tech.bytesmind.logistics.shared.event.model.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement émis lors de l'assignation d'un rôle à un utilisateur.
 */
public record RoleAssignedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID agencyId,
        UUID userId,
        UUID roleId,
        String roleCode
) implements DomainEvent {

    public RoleAssignedEvent(UUID userId, UUID roleId, String roleCode, UUID agencyId) {
        this(UUID.randomUUID(), Instant.now(), agencyId, userId, roleId, roleCode);
    }

    @Override
    public String eventType() {
        return "RoleAssigned";
    }
}