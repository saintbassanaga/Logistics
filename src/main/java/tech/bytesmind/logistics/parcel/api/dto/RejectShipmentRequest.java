package tech.bytesmind.logistics.parcel.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO pour le rejet d'un envoi client par un employé d'agence.
 */
public record RejectShipmentRequest(
        @NotBlank(message = "Rejection reason is required")
        @Size(max = 1000, message = "Rejection reason must not exceed 1000 characters")
        String reason
) {
}