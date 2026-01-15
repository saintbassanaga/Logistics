package tech.bytesmind.logistics.auth.domain.event;


import tech.bytesmind.logistics.shared.event.model.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement émis lors de l'inscription d'un nouvel utilisateur (self-registration).
 */
public record UserRegisteredEvent(
        UUID eventId,
        Instant occurredAt,
        UUID agencyId,
        UUID userId,
        String username,
        String email,
        String firstName,
        String lastName,
        String actorType
) implements DomainEvent {

    public UserRegisteredEvent(UUID userId, String username, String email, String firstName, String lastName, String actorType) {
        this(UUID.randomUUID(), Instant.now(), null, userId, username, email, firstName, lastName, actorType);
    }

    @Override
    public String eventType() {
        return "UserRegistered";
    }
}