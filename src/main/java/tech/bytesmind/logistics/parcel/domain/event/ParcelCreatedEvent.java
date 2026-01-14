package tech.bytesmind.logistics.parcel.domain.event;

import tech.bytesmind.logistics.shared.event.model.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement émis lors de la création d'un Parcel.
 */
public record ParcelCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID agencyId,
        UUID parcelId,
        String trackingNumber,
        UUID shipmentId
) implements DomainEvent {

    public ParcelCreatedEvent(UUID agencyId, UUID parcelId, String trackingNumber, UUID shipmentId) {
        this(UUID.randomUUID(), Instant.now(), agencyId, parcelId, trackingNumber, shipmentId);
    }

    @Override
    public String eventType() {
        return "ParcelCreated";
    }
}