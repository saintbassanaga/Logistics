package tech.bytesmind.logistics.parcel.application.service.impls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.bytesmind.logistics.parcel.application.service.ParcelService;
import tech.bytesmind.logistics.parcel.domain.event.ParcelCreatedEvent;
import tech.bytesmind.logistics.parcel.domain.event.ParcelDeliveredEvent;
import tech.bytesmind.logistics.parcel.domain.event.ParcelStatusChangedEvent;
import tech.bytesmind.logistics.parcel.domain.model.Parcel;
import tech.bytesmind.logistics.parcel.domain.model.ParcelStatus;
import tech.bytesmind.logistics.parcel.domain.model.Shipment;
import tech.bytesmind.logistics.parcel.domain.service.ParcelDomainService;
import tech.bytesmind.logistics.parcel.domain.service.ShipmentDomainService;
import tech.bytesmind.logistics.parcel.domain.service.TrackingNumberGenerator;
import tech.bytesmind.logistics.parcel.infrastructure.repository.ParcelRepository;
import tech.bytesmind.logistics.parcel.infrastructure.repository.ShipmentRepository;
import tech.bytesmind.logistics.shared.event.publisher.TransactionalEventPublisher;
import tech.bytesmind.logistics.shared.exceptions.BusinessException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implémentation du service applicatif pour les Parcels.
 * Orchestration de la logique métier et publication d'événements.
 */
@Service
public class ParcelServiceImpl implements ParcelService {

    private static final Logger log = LoggerFactory.getLogger(ParcelServiceImpl.class);

    private final ParcelRepository parcelRepository;
    private final ShipmentRepository shipmentRepository;
    private final ParcelDomainService parcelDomainService;
    private final ShipmentDomainService shipmentDomainService;
    private final TrackingNumberGenerator trackingNumberGenerator;
    private final TransactionalEventPublisher eventPublisher;

    public ParcelServiceImpl(
            ParcelRepository parcelRepository,
            ShipmentRepository shipmentRepository,
            ParcelDomainService parcelDomainService,
            ShipmentDomainService shipmentDomainService,
            TrackingNumberGenerator trackingNumberGenerator,
            TransactionalEventPublisher eventPublisher
    ) {
        this.parcelRepository = parcelRepository;
        this.shipmentRepository = shipmentRepository;
        this.parcelDomainService = parcelDomainService;
        this.shipmentDomainService = shipmentDomainService;
        this.trackingNumberGenerator = trackingNumberGenerator;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Parcel createParcel(UUID shipmentId, Parcel parcel) {
        log.info("Creating parcel for shipment: {}", shipmentId);

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException("Shipment not found: " + shipmentId));

        // Vérifier que le shipment peut accepter des parcels
        shipmentDomainService.validateCanAddParcel(shipment);

        // Valider les données du parcel
        parcelDomainService.validateParcelData(parcel);

        // Générer le tracking number automatiquement
        String trackingNumber = trackingNumberGenerator.generateUniqueTrackingNumber();
        parcel.setTrackingNumber(trackingNumber);
        log.info("Generated tracking number: {}", trackingNumber);

        // Associer au shipment et à l'agence
        parcel.setShipment(shipment);
        parcel.setAgencyId(shipment.getAgencyId());

        Parcel saved = parcelRepository.save(parcel);

        // Publier événement
        eventPublisher.publish(new ParcelCreatedEvent(
                saved.getAgencyId(),
                saved.getId(),
                saved.getTrackingNumber(),
                shipmentId
        ));

        log.info("Parcel created successfully: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Parcel getParcelById(UUID id) {
        return parcelRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Parcel not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Parcel getParcelByTrackingNumber(String trackingNumber) {
        return parcelRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new BusinessException("Parcel not found: " + trackingNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Parcel> listParcelsByShipment(UUID shipmentId) {
        return parcelRepository.findByShipmentId(shipmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Parcel> listParcelsByAgency(UUID agencyId) {
        return parcelRepository.findByAgencyId(agencyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Parcel> listActiveParcels(UUID agencyId) {
        return parcelRepository.findActiveParcelsByAgency(agencyId);
    }

    @Override
    @Transactional
    public void updateParcelStatus(UUID parcelId, ParcelStatus newStatus, UUID locationId) {
        log.info("Updating parcel status: {} -> {}", parcelId, newStatus);

        Parcel parcel = getParcelById(parcelId);
        ParcelStatus oldStatus = parcel.getStatus();

        parcelDomainService.changeStatus(parcel, newStatus, locationId);
        parcelRepository.save(parcel);

        // Publier événement
        eventPublisher.publish(new ParcelStatusChangedEvent(
                parcel.getAgencyId(),
                parcel.getId(),
                parcel.getTrackingNumber(),
                oldStatus,
                newStatus,
                locationId
        ));

        log.info("Parcel status updated: {}", parcelId);
    }

    @Override
    @Transactional
    public void markParcelAsDelivered(UUID parcelId, String receivedBy) {
        log.info("Marking parcel as delivered: {}", parcelId);

        Parcel parcel = getParcelById(parcelId);
        parcelDomainService.markAsDelivered(parcel, receivedBy);

        parcelRepository.save(parcel);

        // Publier événement
        eventPublisher.publish(new ParcelDeliveredEvent(
                parcel.getAgencyId(),
                parcel.getId(),
                parcel.getTrackingNumber(),
                Instant.now(),
                receivedBy
        ));

        log.info("Parcel marked as delivered: {}", parcelId);
    }

    @Override
    @Transactional
    public void markParcelAsFailed(UUID parcelId, String reason) {
        log.info("Marking parcel as failed: {}", parcelId);

        Parcel parcel = getParcelById(parcelId);
        ParcelStatus oldStatus = parcel.getStatus();

        parcelDomainService.markAsFailed(parcel, reason);
        parcelRepository.save(parcel);

        // Publier événement de changement de statut
        eventPublisher.publish(new ParcelStatusChangedEvent(
                parcel.getAgencyId(),
                parcel.getId(),
                parcel.getTrackingNumber(),
                oldStatus,
                ParcelStatus.FAILED,
                parcel.getCurrentLocationId()
        ));

        log.info("Parcel marked as failed: {}", parcelId);
    }

    @Override
    @Transactional
    public Parcel updateParcel(UUID parcelId, Parcel updatedData) {
        log.info("Updating parcel: {}", parcelId);

        Parcel existing = getParcelById(parcelId);
        parcelDomainService.validateModifiable(existing);

        // Mise à jour des champs modifiables
        existing.setWeight(updatedData.getWeight());
        existing.setLength(updatedData.getLength());
        existing.setWidth(updatedData.getWidth());
        existing.setHeight(updatedData.getHeight());
        existing.setDescription(updatedData.getDescription());
        existing.setDeclaredValue(updatedData.getDeclaredValue());
        existing.setCurrency(updatedData.getCurrency());
        existing.setSpecificReceiverName(updatedData.getSpecificReceiverName());
        existing.setSpecificReceiverPhone(updatedData.getSpecificReceiverPhone());
        existing.setSpecificReceiverAddress(updatedData.getSpecificReceiverAddress());
        existing.setNotes(updatedData.getNotes());

        Parcel updated = parcelRepository.save(existing);
        log.info("Parcel updated: {}", parcelId);

        return updated;
    }
}
