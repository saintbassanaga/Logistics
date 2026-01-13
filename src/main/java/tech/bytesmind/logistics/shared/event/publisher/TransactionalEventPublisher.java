package tech.bytesmind.logistics.shared.event.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tech.bytesmind.logistics.shared.event.model.DomainEvent;


@Component
public class TransactionalEventPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(TransactionalEventPublisher.class);
    
    private final ApplicationEventPublisher applicationEventPublisher;
    
    public TransactionalEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
    
    /**
     * Publie un événement métier.
     * L'événement sera traité APRÈS le commit de la transaction.
     */
    @Transactional
    public void publish(DomainEvent event) {
        log.debug("Publishing domain event: {} [{}]", event.eventType(), event.eventId());
        applicationEventPublisher.publishEvent(event);
    }
}