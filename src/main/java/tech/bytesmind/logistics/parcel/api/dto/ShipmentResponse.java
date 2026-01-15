package tech.bytesmind.logistics.parcel.api.dto;

import tech.bytesmind.logistics.parcel.domain.model.ShipmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShipmentResponse(
        UUID id,
        UUID agencyId,
        String shipmentNumber,
        ShipmentStatus status,

        // Customer tracking
        UUID customerId,
        UUID pickupLocationId,
        UUID validatedById,
        Instant validatedAt,
        String rejectionReason,

        // Sender
        String senderName,
        String senderPhone,
        String senderEmail,
        String senderAddressLine1,
        String senderAddressLine2,
        String senderCity,
        String senderPostalCode,
        String senderCountry,

        // Receiver
        String receiverName,
        String receiverPhone,
        String receiverEmail,
        String receiverAddressLine1,
        String receiverAddressLine2,
        String receiverCity,
        String receiverPostalCode,
        String receiverCountry,

        // Commercial
        BigDecimal totalWeight,
        BigDecimal declaredValue,
        String currency,
        String notes,

        // Metadata
        int parcelCount,
        Instant createdAt,
        Instant updatedAt,
        Instant confirmedAt,
        List<ParcelResponse> parcels
) {
}
