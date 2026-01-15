package tech.bytesmind.logistics.parcel.domain.model;

/**
 * Statut d'un Shipment (envoi groupé).
 * Cycle de vie:
 * - Pour les envois créés par des clients: PENDING_VALIDATION → OPEN → CONFIRMED
 * - Pour les envois créés par l'agence: OPEN → CONFIRMED
 * - Rejet possible: PENDING_VALIDATION → REJECTED
 */
public enum ShipmentStatus {
    /**
     * Shipment créé par un client, en attente de validation par un employé d'agence.
     * Le client doit se présenter au point de collecte pour validation.
     */
    PENDING_VALIDATION,

    /**
     * Shipment validé et en cours de constitution.
     * Des parcels peuvent être ajoutés.
     */
    OPEN,

    /**
     * Shipment confirmé et prêt pour traitement.
     * Aucun parcel ne peut être ajouté.
     */
    CONFIRMED,

    /**
     * Shipment rejeté par l'agence (non conforme, annulé, etc.).
     */
    REJECTED
}