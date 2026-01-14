package tech.bytesmind.logistics.parcel.application.service;

import tech.bytesmind.logistics.parcel.domain.model.Shipment;

import java.util.List;
import java.util.UUID;

/**
 * Interface du service applicatif pour les Shipments.
 */
public interface ShipmentService {

    /**
     * Crée un nouveau Shipment.
     */
    Shipment createShipment(UUID agencyId, Shipment shipment);

    /**
     * Récupère un Shipment par son ID.
     */
    Shipment getShipmentById(UUID id);

    /**
     * Récupère un Shipment avec ses parcels.
     */
    Shipment getShipmentWithParcels(UUID id);

    /**
     * Liste tous les Shipments d'une agence.
     */
    List<Shipment> listShipmentsByAgency(UUID agencyId);

    /**
     * Liste les Shipments OPEN d'une agence.
     */
    List<Shipment> listOpenShipments(UUID agencyId);

    /**
     * Liste les Shipments CONFIRMED d'une agence.
     */
    List<Shipment> listConfirmedShipments(UUID agencyId);

    /**
     * Confirme un Shipment (OPEN → CONFIRMED).
     */
    void confirmShipment(UUID shipmentId);

    /**
     * Met à jour un Shipment (uniquement si OPEN).
     */
    Shipment updateShipment(UUID shipmentId, Shipment updatedData);
}