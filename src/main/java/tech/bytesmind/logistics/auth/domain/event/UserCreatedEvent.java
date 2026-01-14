package tech.bytesmind.logistics.auth.domain.event;

import tech.bytesmind.logistics.shared.event.model.DomainEvent;
import tech.bytesmind.logistics.shared.security.model.ActorType;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement émis lors de la création d'un utilisateur.
 */
public record UserCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID agencyId,
        UUID userId,
        String email,
        ActorType actorType
) implements DomainEvent {

    public UserCreatedEvent(UUID userId, String email, ActorType actorType, UUID agencyId) {
        this(UUID.randomUUID(), Instant.now(), agencyId, userId, email, actorType);
    }

    @Override
    public String eventType() {
        return "UserCreated";
    }
}