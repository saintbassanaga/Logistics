package tech.bytesmind.logistics.parcel.application.service.impls;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.bytesmind.logistics.parcel.application.service.ShipmentValidationService;
import tech.bytesmind.logistics.parcel.domain.event.ShipmentRejectedEvent;
import tech.bytesmind.logistics.parcel.domain.event.ShipmentValidatedEvent;
import tech.bytesmind.logistics.parcel.domain.model.Shipment;
import tech.bytesmind.logistics.parcel.domain.model.ShipmentStatus;
import tech.bytesmind.logistics.parcel.domain.service.ShipmentDomainService;
import tech.bytesmind.logistics.parcel.infrastructure.repository.ShipmentRepository;
import tech.bytesmind.logistics.shared.event.publisher.TransactionalEventPublisher;
import tech.bytesmind.logistics.shared.exceptions.BusinessException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implémentation du service de validation des envois clients.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ShipmentValidationServiceImpl implements ShipmentValidationService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentValidationServiceImpl.class);

    private final ShipmentRepository shipmentRepository;
    private final ShipmentDomainService shipmentDomainService;
    private final TransactionalEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<Shipment> listPendingValidation(UUID agencyId) {
        log.debug("Listing pending validation shipments for agency: {}", agencyId);
        return shipmentRepository.findPendingValidationByAgencyId(agencyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shipment> listPendingValidationByLocation(UUID locationId) {
        log.debug("Listing pending validation shipments for location: {}", locationId);
        return shipmentRepository.findPendingValidationByLocationId(locationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Shipment getPendingShipment(UUID agencyId, UUID shipmentId) {
        log.debug("Getting pending shipment {} for agency: {}", shipmentId, agencyId);

        Shipment shipment = shipmentRepository.findByIdAndAgencyId(shipmentId, agencyId)
                .orElseThrow(() -> new BusinessException("Shipment not found"));

        if (shipment.getStatus() != ShipmentStatus.PENDING_VALIDATION) {
            throw new BusinessException("Shipment is not pending validation");
        }

        return shipment;
    }

    @Override
    public Shipment validateShipment(UUID agencyId, UUID shipmentId, UUID validatorId, String notes) {
        log.info("Validating shipment {} by employee {}", shipmentId, validatorId);

        Shipment shipment = getPendingShipment(agencyId, shipmentId);

        // Valider via le domain service
        shipmentDomainService.validateCustomerShipment(shipment, validatorId);

        // Ajouter les notes si fournies
        if (notes != null && !notes.isBlank()) {
            String existingNotes = shipment.getNotes();
            String newNotes = existingNotes != null
                    ? existingNotes + "\n[Validation] " + notes
                    : "[Validation] " + notes;
            shipment.setNotes(newNotes);
        }

        // Sauvegarder
        shipment = shipmentRepository.save(shipment);

        // Publier l'événement
        eventPublisher.publish(new ShipmentValidatedEvent(
                shipment.getId(),
                shipment.getShipmentNumber(),
                shipment.getAgencyId(),
                shipment.getCustomerId(),
                validatorId,
                shipment.getPickupLocationId(),
                Instant.now()
        ));

        log.info("Shipment {} validated successfully", shipmentId);
        return shipment;
    }

    @Override
    public Shipment rejectShipment(UUID agencyId, UUID shipmentId, UUID rejectorId, String reason) {
        log.info("Rejecting shipment {} by employee {}", shipmentId, rejectorId);

        Shipment shipment = getPendingShipment(agencyId, shipmentId);

        // Rejeter via le domain service
        shipmentDomainService.rejectCustomerShipment(shipment, rejectorId, reason);

        // Sauvegarder
        shipment = shipmentRepository.save(shipment);

        // Publier l'événement
        eventPublisher.publish(new ShipmentRejectedEvent(
                shipment.getId(),
                shipment.getShipmentNumber(),
                shipment.getAgencyId(),
                shipment.getCustomerId(),
                rejectorId,
                reason,
                Instant.now()
        ));

        log.info("Shipment {} rejected", shipmentId);
        return shipment;
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingValidation(UUID agencyId) {
        return shipmentRepository.countByAgencyIdAndStatus(agencyId, ShipmentStatus.PENDING_VALIDATION);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingValidationByLocation(UUID locationId) {
        return shipmentRepository.countPendingValidationByLocationId(locationId);
    }
}