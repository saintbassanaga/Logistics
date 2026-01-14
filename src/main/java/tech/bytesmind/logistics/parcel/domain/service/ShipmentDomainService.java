package tech.bytesmind.logistics.parcel.domain.service;

import org.springframework.stereotype.Service;
import tech.bytesmind.logistics.parcel.domain.model.Parcel;
import tech.bytesmind.logistics.parcel.domain.model.Shipment;
import tech.bytesmind.logistics.parcel.domain.model.ShipmentStatus;
import tech.bytesmind.logistics.shared.exceptions.BusinessException;

import java.time.Instant;

/**
 * Service de domaine pour la logique métier des Shipments.
 * Implémente les invariants et règles métier.
 */
@Service
public class ShipmentDomainService {

    /**
     * Confirme un Shipment.
     * Transition : OPEN → CONFIRMED.
     */
    public void confirm(Shipment shipment) {
        if (shipment.getStatus() != ShipmentStatus.OPEN) {
            throw new BusinessException("Only OPEN shipments can be confirmed");
        }

        if (shipment.getParcels() == null || shipment.getParcels().isEmpty()) {
            throw new BusinessException("Cannot confirm shipment without parcels");
        }

        shipment.setStatus(ShipmentStatus.CONFIRMED);
        shipment.setConfirmedAt(Instant.now());
    }

    /**
     * Vérifie si un Shipment peut accepter de nouveaux parcels.
     */
    public boolean canAddParcel(Shipment shipment) {
        return shipment.getStatus() == ShipmentStatus.OPEN;
    }

    /**
     * Valide qu'un parcel peut être ajouté au shipment.
     */
    public void validateCanAddParcel(Shipment shipment) {
        if (!canAddParcel(shipment)) {
            throw new BusinessException("Cannot add parcel to " + shipment.getStatus() + " shipment");
        }
    }

    /**
     * Vérifie si un Shipment est modifiable.
     */
    public boolean isModifiable(Shipment shipment) {
        return shipment.getStatus() == ShipmentStatus.OPEN;
    }

    /**
     * Valide qu'un Shipment peut être modifié.
     */
    public void validateModifiable(Shipment shipment) {
        if (!isModifiable(shipment)) {
            throw new BusinessException("Cannot modify " + shipment.getStatus() + " shipment");
        }
    }

    /**
     * Compte le nombre de parcels dans un Shipment.
     */
    public int countParcels(Shipment shipment) {
        if (shipment.getParcels() == null) {
            return 0;
        }
        return shipment.getParcels().size();
    }

    /**
     * Vérifie si un Shipment a au moins un parcel.
     */
    public boolean hasParcels(Shipment shipment) {
        return countParcels(shipment) > 0;
    }

    /**
     * Valide les données d'un Shipment avant création.
     */
    public void validateShipmentData(Shipment shipment) {
        if (shipment.getSenderName() == null || shipment.getSenderName().isBlank()) {
            throw new BusinessException("Sender name is required");
        }

        if (shipment.getReceiverName() == null || shipment.getReceiverName().isBlank()) {
            throw new BusinessException("Receiver name is required");
        }

        if (shipment.getSenderCountry() == null || shipment.getSenderCountry().length() != 2) {
            throw new BusinessException("Valid sender country code (ISO 3166-1 alpha-2) is required");
        }

        if (shipment.getReceiverCountry() == null || shipment.getReceiverCountry().length() != 2) {
            throw new BusinessException("Valid receiver country code (ISO 3166-1 alpha-2) is required");
        }
    }

    /**
     * Génère un numéro de Shipment unique.
     */
    public String generateShipmentNumber(String agencyCode) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.valueOf((int) (Math.random() * 10000));
        return String.format("SHP-%s-%s-%s", agencyCode, timestamp, random);
    }
}