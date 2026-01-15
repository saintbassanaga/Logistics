package tech.bytesmind.logistics.parcel.domain.event;

import tech.bytesmind.logistics.shared.event.model.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement émis lors du rejet d'un envoi par un employé d'agence.
 */
public record ShipmentRejectedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID agencyId,
        UUID shipmentId,
        String shipmentNumber,
        UUID customerId,
        UUID rejectedById,
        String rejectionReason
) implements DomainEvent {

    public ShipmentRejectedEvent(
            UUID shipmentId,
            String shipmentNumber,
            UUID agencyId,
            UUID customerId,
            UUID rejectedById,
            String rejectionReason,
            Instant rejectedAt
    ) {
        this(UUID.randomUUID(), rejectedAt, agencyId, shipmentId, shipmentNumber, customerId, rejectedById, rejectionReason);
    }

    @Override
    public String eventType() {
        return "ShipmentRejected";
    }
}