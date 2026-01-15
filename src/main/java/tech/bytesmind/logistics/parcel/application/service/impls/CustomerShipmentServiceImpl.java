package tech.bytesmind.logistics.parcel.application.service.impls;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.bytesmind.logistics.parcel.api.dto.CustomerShipmentRequest;
import tech.bytesmind.logistics.parcel.application.mapper.ShipmentMapper;
import tech.bytesmind.logistics.parcel.application.service.CustomerShipmentService;
import tech.bytesmind.logistics.parcel.domain.event.ShipmentCreatedEvent;
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
 * Implémentation du service applicatif pour les envois créés par les clients.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerShipmentServiceImpl implements CustomerShipmentService {

    private static final Logger log = LoggerFactory.getLogger(CustomerShipmentServiceImpl.class);

    private final ShipmentRepository shipmentRepository;
    private final ShipmentDomainService shipmentDomainService;
    private final ShipmentMapper shipmentMapper;
    private final TransactionalEventPublisher eventPublisher;

    @Override
    public Shipment createShipment(UUID customerId, CustomerShipmentRequest request) {
        log.info("Creating customer shipment for customer: {}", customerId);

        // Mapper le DTO vers l'entité
        Shipment shipment = shipmentMapper.toEntity(request);

        // Générer le numéro d'envoi
        String shipmentNumber = shipmentDomainService.generateShipmentNumber("CUST");
        shipment.setShipmentNumber(shipmentNumber);

        // Initialiser comme envoi client (PENDING_VALIDATION)
        shipmentDomainService.initializeCustomerShipment(shipment, customerId, request.pickupLocationId());

        // Valider les données
        shipmentDomainService.validateShipmentData(shipment);

        // Sauvegarder
        shipment = shipmentRepository.save(shipment);

        // Publier l'événement
        eventPublisher.publish(new ShipmentCreatedEvent(
                shipment.getAgencyId(),
                shipment.getId(),
                shipment.getShipmentNumber()
        ));

        log.info("Customer shipment created: {} ({})", shipment.getShipmentNumber(), shipment.getId());
        return shipment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shipment> listCustomerShipments(UUID customerId) {
        log.debug("Listing shipments for customer: {}", customerId);
        return shipmentRepository.findByCustomerId(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shipment> listCustomerShipmentsByStatus(UUID customerId, String status) {
        log.debug("Listing shipments for customer: {} with status: {}", customerId, status);

        ShipmentStatus shipmentStatus;
        try {
            shipmentStatus = ShipmentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid status: " + status);
        }

        return shipmentRepository.findByCustomerIdAndStatus(customerId, shipmentStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public Shipment getCustomerShipment(UUID customerId, UUID shipmentId) {
        log.debug("Getting shipment {} for customer: {}", shipmentId, customerId);

        return shipmentRepository.findByIdAndCustomerId(shipmentId, customerId)
                .orElseThrow(() -> new BusinessException("Shipment not found or access denied"));
    }

    @Override
    public void cancelShipment(UUID customerId, UUID shipmentId) {
        log.info("Canceling shipment {} for customer: {}", shipmentId, customerId);

        Shipment shipment = getCustomerShipment(customerId, shipmentId);

        // Valider que le client peut annuler
        shipmentDomainService.validateCustomerCanCancel(shipment);

        // Supprimer l'envoi (soft delete)
        shipmentRepository.delete(shipment);

        log.info("Shipment {} canceled by customer: {}", shipmentId, customerId);
    }

    @Override
    public Shipment updateShipment(UUID customerId, UUID shipmentId, CustomerShipmentRequest request) {
        log.info("Updating shipment {} for customer: {}", shipmentId, customerId);

        Shipment shipment = getCustomerShipment(customerId, shipmentId);

        // Valider que le client peut modifier
        shipmentDomainService.validateCustomerCanModify(shipment);

        // Mettre à jour via le mapper
        shipmentMapper.updateFromCustomerRequest(request, shipment);

        // Valider les données
        shipmentDomainService.validateShipmentData(shipment);

        // Sauvegarder
        shipment = shipmentRepository.save(shipment);

        log.info("Shipment {} updated by customer: {}", shipmentId, customerId);
        return shipment;
    }
}