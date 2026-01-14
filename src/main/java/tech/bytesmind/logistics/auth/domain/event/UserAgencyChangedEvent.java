package tech.bytesmind.logistics.auth.domain.event;

import tech.bytesmind.logistics.shared.event.model.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement émis lors du changement d'agence d'un employé.
 * Important pour invalidation de JWT et notification.
 */
public record UserAgencyChangedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID agencyId, // Nouvelle agence
        UUID userId,
        UUID oldAgencyId,
        UUID newAgencyId
) implements DomainEvent {

    public UserAgencyChangedEvent(UUID userId, UUID oldAgencyId, UUID newAgencyId) {
        this(UUID.randomUUID(), Instant.now(), newAgencyId, userId, oldAgencyId, newAgencyId);
    }

    @Override
    public String eventType() {
        return "UserAgencyChanged";
    }
}