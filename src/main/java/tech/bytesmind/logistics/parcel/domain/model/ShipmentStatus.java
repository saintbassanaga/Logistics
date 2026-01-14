package tech.bytesmind.logistics.parcel.domain.model;

/**
 * Statut d'un Shipment (envoi groupé).
 * Cycle de vie court : OPEN → CONFIRMED.
 */
public enum ShipmentStatus {
    /**
     * Shipment en cours de constitution.
     * Des parcels peuvent être ajoutés.
     */
    OPEN,

    /**
     * Shipment confirmé et prêt pour traitement.
     * Aucun parcel ne peut être ajouté.
     */
    CONFIRMED
}