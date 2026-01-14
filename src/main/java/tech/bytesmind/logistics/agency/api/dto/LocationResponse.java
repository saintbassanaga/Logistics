package tech.bytesmind.logistics.agency.api.dto;


import tech.bytesmind.logistics.agency.domain.model.LocationType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LocationResponse(
        UUID id, UUID agencyId, String code, String name, LocationType locationType,
        String addressLine1, String addressLine2, String city, String stateRegion, String postalCode, String country,
        BigDecimal latitude, BigDecimal longitude,
        String email, String phone, String contactPersonName, String contactPersonPhone,
        String openingHours,
        Integer maxDailyParcels, BigDecimal storageCapacityM3,
        boolean active, boolean temporarilyClosed, String closureReason, boolean operational,
        Instant createdAt, Instant updatedAt
) {
}
