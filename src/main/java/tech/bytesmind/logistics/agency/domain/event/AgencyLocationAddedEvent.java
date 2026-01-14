package tech.bytesmind.logistics.agency.domain.event;


import tech.bytesmind.logistics.shared.event.model.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement émis lors de l'ajout d'une localisation à une agence.
 */
public record AgencyLocationAddedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID agencyId,
        UUID locationId,
        String locationCode,
        String city
) implements DomainEvent {

    public AgencyLocationAddedEvent(UUID agencyId, UUID locationId, String locationCode, String city) {
        this(UUID.randomUUID(), Instant.now(), agencyId, locationId, locationCode, city);
    }

    @Override
    public String eventType() {
        return "AgencyLocationAdded";
    }
}
