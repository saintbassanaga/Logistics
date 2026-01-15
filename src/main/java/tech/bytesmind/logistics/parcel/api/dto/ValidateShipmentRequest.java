package tech.bytesmind.logistics.parcel.api.dto;

/**
 * DTO pour la validation d'un envoi client par un employé d'agence.
 * Pas de champs requis - la validation est une simple approbation.
 */
public record ValidateShipmentRequest(
        String notes
) {
}