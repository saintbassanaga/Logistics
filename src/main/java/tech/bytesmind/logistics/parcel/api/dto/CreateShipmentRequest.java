package tech.bytesmind.logistics.parcel.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateShipmentRequest(
        @NotBlank @Size(max = 255) String senderName,
        @Size(max = 50) String senderPhone,
        @Size(max = 255) String senderEmail,
        @NotBlank @Size(max = 255) String senderAddressLine1,
        @Size(max = 255) String senderAddressLine2,
        @NotBlank @Size(max = 100) String senderCity,
        @NotBlank @Size(max = 20) String senderPostalCode,
        @NotBlank @Size(min = 2, max = 2) String senderCountry,
        @NotBlank @Size(max = 255) String receiverName,
        @Size(max = 50) String receiverPhone,
        @Size(max = 255) String receiverEmail,
        @NotBlank @Size(max = 255) String receiverAddressLine1,
        @Size(max = 255) String receiverAddressLine2,
        @NotBlank @Size(max = 100) String receiverCity,
        @NotBlank @Size(max = 20) String receiverPostalCode,
        @NotBlank @Size(min = 2, max = 2) String receiverCountry,
        BigDecimal declaredValue,
        @Size(min = 3, max = 3) String currency,
        String notes
) {
}
