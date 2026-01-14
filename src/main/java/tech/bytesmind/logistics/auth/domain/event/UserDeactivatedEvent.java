package tech.bytesmind.logistics.auth.domain.event;

import tech.bytesmind.logistics.shared.event.model.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement émis lors de la désactivation d'un utilisateur.
 */
public record UserDeactivatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID agencyId,
        UUID userId,
        String reason
) implements DomainEvent {

    public UserDeactivatedEvent(UUID userId, String reason, UUID agencyId) {
        this(UUID.randomUUID(), Instant.now(), agencyId, userId, reason);
    }

    @Override
    public String eventType() {
        return "UserDeactivated";
    }
}