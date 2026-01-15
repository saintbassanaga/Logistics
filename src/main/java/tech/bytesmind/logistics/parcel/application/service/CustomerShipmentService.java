package tech.bytesmind.logistics.parcel.application.service;

import tech.bytesmind.logistics.parcel.api.dto.CustomerShipmentRequest;
import tech.bytesmind.logistics.parcel.domain.model.Shipment;

import java.util.List;
import java.util.UUID;

/**
 * Interface du service applicatif pour les envois créés par les clients.
 * Les envois clients sont créés avec le statut PENDING_VALIDATION.
 */
public interface CustomerShipmentService {

    /**
     * Crée un nouvel envoi pour un client.
     * L'envoi est créé avec le statut PENDING_VALIDATION.
     *
     * @param customerId ID du client
     * @param request Données de l'envoi
     * @return L'envoi créé
     */
    Shipment createShipment(UUID customerId, CustomerShipmentRequest request);

    /**
     * Liste tous les envois d'un client.
     */
    List<Shipment> listCustomerShipments(UUID customerId);

    /**
     * Liste les envois d'un client par statut.
     */
    List<Shipment> listCustomerShipmentsByStatus(UUID customerId, String status);

    /**
     * Récupère un envoi par ID (vérifie que le client en est propriétaire).
     */
    Shipment getCustomerShipment(UUID customerId, UUID shipmentId);

    /**
     * Annule un envoi en attente de validation.
     * Seuls les envois PENDING_VALIDATION peuvent être annulés par le client.
     */
    void cancelShipment(UUID customerId, UUID shipmentId);

    /**
     * Met à jour un envoi en attente de validation.
     * Seuls les envois PENDING_VALIDATION peuvent être modifiés par le client.
     */
    Shipment updateShipment(UUID customerId, UUID shipmentId, CustomerShipmentRequest request);
}