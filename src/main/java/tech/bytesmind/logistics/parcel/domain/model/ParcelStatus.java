package tech.bytesmind.logistics.parcel.domain.model;

/**
 * Statut d'un Parcel (colis individuel).
 * Lifecycle complet et indépendant avec tracking.
 */
public enum ParcelStatus {
    /**
     * Parcel enregistré dans le système.
     */
    REGISTERED,

    /**
     * Parcel en transit vers destination.
     */
    IN_TRANSIT,

    /**
     * Parcel en centre de tri.
     */
    IN_SORTING,

    /**
     * Parcel en cours de livraison finale.
     */
    OUT_FOR_DELIVERY,

    /**
     * Parcel livré avec succès.
     */
    DELIVERED,

    /**
     * Échec de livraison.
     */
    FAILED,

    /**
     * Parcel en cours de retour à l'expéditeur.
     */
    RETURNED
}