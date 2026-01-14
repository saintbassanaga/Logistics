package tech.bytesmind.logistics.parcel.domain.event;

import tech.bytesmind.logistics.shared.event.model.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement émis lors de la livraison réussie d'un Parcel.
 */
public record ParcelDeliveredEvent(
        UUID eventId,
        Instant occurredAt,
        UUID agencyId,
        UUID parcelId,
        String trackingNumber,
        Instant deliveredAt,
        String receivedBy
) implements DomainEvent {

    public ParcelDeliveredEvent(
            UUID agencyId,
            UUID parcelId,
            String trackingNumber,
            Instant deliveredAt,
            String receivedBy
    ) {
        this(UUID.randomUUID(), Instant.now(), agencyId, parcelId, trackingNumber, deliveredAt, receivedBy);
    }

    @Override
    public String eventType() {
        return "ParcelDelivered";
    }
}