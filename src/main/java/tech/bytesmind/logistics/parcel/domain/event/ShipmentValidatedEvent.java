package tech.bytesmind.logistics.parcel.domain.event;

import tech.bytesmind.logistics.shared.event.model.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement émis lors de la validation d'un envoi par un employé d'agence.
 */
public record ShipmentValidatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID agencyId,
        UUID shipmentId,
        String shipmentNumber,
        UUID customerId,
        UUID validatedById,
        UUID pickupLocationId
) implements DomainEvent {

    public ShipmentValidatedEvent(
            UUID shipmentId,
            String shipmentNumber,
            UUID agencyId,
            UUID customerId,
            UUID validatedById,
            UUID pickupLocationId,
            Instant validatedAt
    ) {
        this(UUID.randomUUID(), validatedAt, agencyId, shipmentId, shipmentNumber, customerId, validatedById, pickupLocationId);
    }

    @Override
    public String eventType() {
        return "ShipmentValidated";
    }
}