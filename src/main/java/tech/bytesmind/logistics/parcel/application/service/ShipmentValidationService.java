package tech.bytesmind.logistics.parcel.application.service;

import tech.bytesmind.logistics.parcel.domain.model.Shipment;

import java.util.List;
import java.util.UUID;

/**
 * Interface du service applicatif pour la validation des envois clients.
 * Utilisé par les employés d'agence pour valider ou rejeter les envois
 * créés par les clients (statut PENDING_VALIDATION).
 */
public interface ShipmentValidationService {

    /**
     * Liste les envois en attente de validation pour une agence.
     */
    List<Shipment> listPendingValidation(UUID agencyId);

    /**
     * Liste les envois en attente de validation pour un lieu de collecte.
     */
    List<Shipment> listPendingValidationByLocation(UUID locationId);

    /**
     * Récupère un envoi en attente de validation par ID.
     * Vérifie que l'envoi appartient à l'agence spécifiée.
     */
    Shipment getPendingShipment(UUID agencyId, UUID shipmentId);

    /**
     * Valide un envoi client.
     * Transition : PENDING_VALIDATION → OPEN.
     *
     * @param agencyId    ID de l'agence
     * @param shipmentId  ID de l'envoi
     * @param validatorId ID de l'employé qui valide
     * @param notes       Notes optionnelles
     * @return L'envoi validé
     */
    Shipment validateShipment(UUID agencyId, UUID shipmentId, UUID validatorId, String notes);

    /**
     * Rejette un envoi client.
     * Transition : PENDING_VALIDATION → REJECTED.
     *
     * @param agencyId    ID de l'agence
     * @param shipmentId  ID de l'envoi
     * @param rejectorId  ID de l'employé qui rejette
     * @param reason      Raison du rejet
     * @return L'envoi rejeté
     */
    Shipment rejectShipment(UUID agencyId, UUID shipmentId, UUID rejectorId, String reason);

    /**
     * Compte les envois en attente de validation pour une agence.
     */
    long countPendingValidation(UUID agencyId);

    /**
     * Compte les envois en attente de validation pour un lieu de collecte.
     */
    long countPendingValidationByLocation(UUID locationId);
}