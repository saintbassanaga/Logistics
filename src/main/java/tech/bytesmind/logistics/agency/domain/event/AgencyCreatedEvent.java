package tech.bytesmind.logistics.agency.domain.event;

import tech.bytesmind.logistics.shared.event.model.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement émis lors de la création d'une agence.
 */
public record AgencyCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID agencyId,
        String agencyCode,
        String agencyName
) implements DomainEvent {

    public AgencyCreatedEvent(UUID agencyId, String agencyCode, String agencyName) {
        this(UUID.randomUUID(), Instant.now(), agencyId, agencyCode, agencyName);
    }

    @Override
    public String eventType() {
        return "AgencyCreated";
    }
}
