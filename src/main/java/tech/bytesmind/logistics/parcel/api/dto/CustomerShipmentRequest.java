package tech.bytesmind.logistics.parcel.api.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO pour la création d'un envoi par un client.
 * L'envoi sera créé avec le statut PENDING_VALIDATION.
 */
public record CustomerShipmentRequest(
        // Agency et lieu de collecte
        @NotNull(message = "Agency ID is required")
        UUID agencyId,

        @NotNull(message = "Pickup location ID is required")
        UUID pickupLocationId,

        // Expéditeur
        @NotBlank(message = "Sender name is required")
        @Size(max = 255)
        String senderName,

        @Size(max = 50)
        String senderPhone,

        @Email
        String senderEmail,

        @NotBlank(message = "Sender address is required")
        @Size(max = 255)
        String senderAddressLine1,

        @Size(max = 255)
        String senderAddressLine2,

        @NotBlank(message = "Sender city is required")
        @Size(max = 100)
        String senderCity,

        @NotBlank(message = "Sender postal code is required")
        @Size(max = 20)
        String senderPostalCode,

        @NotBlank(message = "Sender country is required")
        @Size(min = 2, max = 2, message = "Country must be a 2-letter ISO code")
        String senderCountry,

        // Destinataire
        @NotBlank(message = "Receiver name is required")
        @Size(max = 255)
        String receiverName,

        @Size(max = 50)
        String receiverPhone,

        @Email
        String receiverEmail,

        @NotBlank(message = "Receiver address is required")
        @Size(max = 255)
        String receiverAddressLine1,

        @Size(max = 255)
        String receiverAddressLine2,

        @NotBlank(message = "Receiver city is required")
        @Size(max = 100)
        String receiverCity,

        @NotBlank(message = "Receiver postal code is required")
        @Size(max = 20)
        String receiverPostalCode,

        @NotBlank(message = "Receiver country is required")
        @Size(min = 2, max = 2, message = "Country must be a 2-letter ISO code")
        String receiverCountry,

        // Informations commerciales
        @DecimalMin(value = "0.001", message = "Weight must be positive")
        BigDecimal totalWeight,

        @DecimalMin(value = "0.00", message = "Declared value must be non-negative")
        BigDecimal declaredValue,

        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency,

        String notes
) {
}