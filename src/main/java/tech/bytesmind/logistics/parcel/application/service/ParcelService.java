package tech.bytesmind.logistics.parcel.application.service;

import tech.bytesmind.logistics.parcel.domain.model.Parcel;
import tech.bytesmind.logistics.parcel.domain.model.ParcelStatus;

import java.util.List;
import java.util.UUID;

/**
 * Interface du service applicatif pour les Parcels.
 */
public interface ParcelService {

    /**
     * Crée un nouveau Parcel dans un Shipment.
     */
    Parcel createParcel(UUID shipmentId, Parcel parcel);

    /**
     * Récupère un Parcel par son ID.
     */
    Parcel getParcelById(UUID id);

    /**
     * Récupère un Parcel par son numéro de tracking.
     */
    Parcel getParcelByTrackingNumber(String trackingNumber);

    /**
     * Liste tous les Parcels d'un Shipment.
     */
    List<Parcel> listParcelsByShipment(UUID shipmentId);

    /**
     * Liste tous les Parcels d'une agence.
     */
    List<Parcel> listParcelsByAgency(UUID agencyId);

    /**
     * Liste les Parcels actifs d'une agence (en transit, tri, livraison).
     */
    List<Parcel> listActiveParcels(UUID agencyId);

    /**
     * Met à jour le statut d'un Parcel.
     */
    void updateParcelStatus(UUID parcelId, ParcelStatus newStatus, UUID locationId);

    /**
     * Marque un Parcel comme livré.
     */
    void markParcelAsDelivered(UUID parcelId, String receivedBy);

    /**
     * Marque un Parcel comme échec de livraison.
     */
    void markParcelAsFailed(UUID parcelId, String reason);

    /**
     * Met à jour un Parcel (uniquement si REGISTERED).
     */
    Parcel updateParcel(UUID parcelId, Parcel updatedData);
}