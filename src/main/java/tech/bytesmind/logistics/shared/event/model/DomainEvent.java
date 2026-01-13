package tech.bytesmind.logistics.shared.event.model;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    
    /**
     * Identifiant unique de l'événement.
     */
    UUID eventId();
    
    /**
     * Timestamp de l'événement.
     */
    Instant occurredAt();
    
    /**
     * Agency ID associé à l'événement (pour multi-tenance).
     */
    UUID agencyId();
    
    /**
     * Type d'événement (pour routing).
     */
    String eventType();
}