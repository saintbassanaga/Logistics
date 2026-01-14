package tech.bytesmind.logistics.agency.domain.event;

import tech.bytesmind.logistics.shared.event.model.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record AgencySuspendedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID agencyId,
        String reason
) implements DomainEvent {

    public AgencySuspendedEvent(UUID agencyId, String reason) {
        this(UUID.randomUUID(), Instant.now(), agencyId, reason);
    }

    @Override
    public String eventType() {
        return "AgencySuspended";
    }
}
