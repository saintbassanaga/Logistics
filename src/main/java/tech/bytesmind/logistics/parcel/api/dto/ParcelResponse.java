package tech.bytesmind.logistics.parcel.api.dto;

import tech.bytesmind.logistics.parcel.domain.model.ParcelStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ParcelResponse(
        UUID id,
        UUID agencyId,
        UUID shipmentId,
        String trackingNumber,
        ParcelStatus status,
        BigDecimal weight,
        BigDecimal length,
        BigDecimal width,
        BigDecimal height,
        String description,
        BigDecimal declaredValue,
        String currency,
        String specificReceiverName,
        String specificReceiverPhone,
        String specificReceiverAddress,
        UUID currentLocationId,
        Instant lastScanAt,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        Instant deliveredAt
) {
}
