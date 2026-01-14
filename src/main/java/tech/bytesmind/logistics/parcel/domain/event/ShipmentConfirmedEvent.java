package tech.bytesmind.logistics.parcel.domain.event;

import tech.bytesmind.logistics.shared.event.model.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement émis lors de la confirmation d'un Shipment.
 * Après cet événement, aucun Parcel ne peut être ajouté.
 */
public record ShipmentConfirmedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID agencyId,
        UUID shipmentId,
        String shipmentNumber,
        int parcelCount
) implements DomainEvent {

    public ShipmentConfirmedEvent(UUID agencyId, UUID shipmentId, String shipmentNumber, int parcelCount) {
        this(UUID.randomUUID(), Instant.now(), agencyId, shipmentId, shipmentNumber, parcelCount);
    }

    @Override
    public String eventType() {
        return "ShipmentConfirmed";
    }
}