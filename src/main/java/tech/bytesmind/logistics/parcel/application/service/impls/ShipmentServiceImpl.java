package tech.bytesmind.logistics.parcel.application.service.impls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.bytesmind.logistics.parcel.application.service.ShipmentService;
import tech.bytesmind.logistics.parcel.domain.event.ShipmentConfirmedEvent;
import tech.bytesmind.logistics.parcel.domain.event.ShipmentCreatedEvent;
import tech.bytesmind.logistics.parcel.domain.model.Shipment;
import tech.bytesmind.logistics.parcel.domain.service.ShipmentDomainService;
import tech.bytesmind.logistics.parcel.domain.service.ShipmentNumberGenerator;
import tech.bytesmind.logistics.parcel.infrastructure.repository.ShipmentRepository;
import tech.bytesmind.logistics.shared.event.publisher.TransactionalEventPublisher;
import tech.bytesmind.logistics.shared.exceptions.BusinessException;

import java.util.List;
import java.util.UUID;

/**
 * Implémentation du service applicatif pour les Shipments.
 * Orchestration de la logique métier et publication d'événements.
 */
@Service
public class ShipmentServiceImpl implements ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentServiceImpl.class);

    private final ShipmentRepository shipmentRepository;
    private final ShipmentDomainService domainService;
    private final ShipmentNumberGenerator numberGenerator;
    private final TransactionalEventPublisher eventPublisher;

    public ShipmentServiceImpl(
            ShipmentRepository shipmentRepository,
            ShipmentDomainService domainService,
            ShipmentNumberGenerator numberGenerator,
            TransactionalEventPublisher eventPublisher
    ) {
        this.shipmentRepository = shipmentRepository;
        this.domainService = domainService;
        this.numberGenerator = numberGenerator;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Shipment createShipment(UUID agencyId, Shipment shipment) {
        log.info("Creating shipment for agency: {}", agencyId);

        shipment.setAgencyId(agencyId);
        domainService.validateShipmentData(shipment);

        // Générer le numéro de shipment automatiquement
        String shipmentNumber = numberGenerator.generateUniqueNumber(agencyId);
        shipment.setShipmentNumber(shipmentNumber);
        log.info("Generated shipment number: {}", shipmentNumber);

        Shipment saved = shipmentRepository.save(shipment);

        // Publier événement
        eventPublisher.publish(new ShipmentCreatedEvent(
                saved.getAgencyId(),
                saved.getId(),
                saved.getShipmentNumber()
        ));

        log.info("Shipment created successfully: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Shipment getShipmentById(UUID id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Shipment not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Shipment getShipmentWithParcels(UUID id) {
        return shipmentRepository.findByIdWithParcels(id)
                .orElseThrow(() -> new BusinessException("Shipment not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shipment> listShipmentsByAgency(UUID agencyId) {
        return shipmentRepository.findByAgencyId(agencyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shipment> listOpenShipments(UUID agencyId) {
        return shipmentRepository.findOpenShipmentsByAgency(agencyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shipment> listConfirmedShipments(UUID agencyId) {
        return shipmentRepository.findConfirmedShipmentsByAgency(agencyId);
    }

    @Override
    @Transactional
    public void confirmShipment(UUID shipmentId) {
        log.info("Confirming shipment: {}", shipmentId);

        Shipment shipment = getShipmentWithParcels(shipmentId);
        domainService.confirm(shipment);

        shipmentRepository.save(shipment);

        // Publier événement
        eventPublisher.publish(new ShipmentConfirmedEvent(
                shipment.getAgencyId(),
                shipment.getId(),
                shipment.getShipmentNumber(),
                domainService.countParcels(shipment)
        ));

        log.info("Shipment confirmed: {}", shipmentId);
    }

    @Override
    @Transactional
    public Shipment updateShipment(UUID shipmentId, Shipment updatedData) {
        log.info("Updating shipment: {}", shipmentId);

        Shipment existing = getShipmentById(shipmentId);
        domainService.validateModifiable(existing);

        // Mise à jour des champs modifiables
        existing.setSenderName(updatedData.getSenderName());
        existing.setSenderPhone(updatedData.getSenderPhone());
        existing.setSenderEmail(updatedData.getSenderEmail());
        existing.setSenderAddressLine1(updatedData.getSenderAddressLine1());
        existing.setSenderAddressLine2(updatedData.getSenderAddressLine2());
        existing.setSenderCity(updatedData.getSenderCity());
        existing.setSenderPostalCode(updatedData.getSenderPostalCode());
        existing.setSenderCountry(updatedData.getSenderCountry());

        existing.setReceiverName(updatedData.getReceiverName());
        existing.setReceiverPhone(updatedData.getReceiverPhone());
        existing.setReceiverEmail(updatedData.getReceiverEmail());
        existing.setReceiverAddressLine1(updatedData.getReceiverAddressLine1());
        existing.setReceiverAddressLine2(updatedData.getReceiverAddressLine2());
        existing.setReceiverCity(updatedData.getReceiverCity());
        existing.setReceiverPostalCode(updatedData.getReceiverPostalCode());
        existing.setReceiverCountry(updatedData.getReceiverCountry());

        existing.setNotes(updatedData.getNotes());

        Shipment updated = shipmentRepository.save(existing);
        log.info("Shipment updated: {}", shipmentId);

        return updated;
    }
}
