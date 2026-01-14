package tech.bytesmind.logistics.parcel.domain.event;

import tech.bytesmind.logistics.shared.event.model.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement émis lors de la création d'un Shipment.
 */
public record ShipmentCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID agencyId,
        UUID shipmentId,
        String shipmentNumber
) implements DomainEvent {

    public ShipmentCreatedEvent(UUID agencyId, UUID shipmentId, String shipmentNumber) {
        this(UUID.randomUUID(), Instant.now(), agencyId, shipmentId, shipmentNumber);
    }

    @Override
    public String eventType() {
        return "ShipmentCreated";
    }
}