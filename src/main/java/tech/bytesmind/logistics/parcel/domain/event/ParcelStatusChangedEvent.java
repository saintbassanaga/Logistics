package tech.bytesmind.logistics.parcel.domain.event;

import tech.bytesmind.logistics.parcel.domain.model.ParcelStatus;
import tech.bytesmind.logistics.shared.event.model.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement émis lors d'un changement de statut d'un Parcel.
 * Utilisé pour le tracking et la notification.
 */
public record ParcelStatusChangedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID agencyId,
        UUID parcelId,
        String trackingNumber,
        ParcelStatus oldStatus,
        ParcelStatus newStatus,
        UUID locationId
) implements DomainEvent {

    public ParcelStatusChangedEvent(
            UUID agencyId,
            UUID parcelId,
            String trackingNumber,
            ParcelStatus oldStatus,
            ParcelStatus newStatus,
            UUID locationId
    ) {
        this(UUID.randomUUID(), Instant.now(), agencyId, parcelId, trackingNumber, oldStatus, newStatus, locationId);
    }

    @Override
    public String eventType() {
        return "ParcelStatusChanged";
    }
}