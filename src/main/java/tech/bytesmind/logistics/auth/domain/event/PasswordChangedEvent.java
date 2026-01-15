package tech.bytesmind.logistics.auth.domain.event;

import tech.bytesmind.logistics.shared.event.model.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement émis lors du changement de mot de passe d'un utilisateur.
 */
public record PasswordChangedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID agencyId,
        UUID userId,
        String email
) implements DomainEvent {

    public PasswordChangedEvent(UUID userId, String email) {
        this(UUID.randomUUID(), Instant.now(), null, userId, email);
    }

    @Override
    public String eventType() {
        return "PasswordChanged";
    }
}