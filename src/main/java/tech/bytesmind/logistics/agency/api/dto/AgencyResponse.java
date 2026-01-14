package tech.bytesmind.logistics.agency.api.dto;

import tech.bytesmind.logistics.agency.domain.model.SubscriptionTier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgencyResponse(
        UUID id, String code, String name, String legalName, String email, String phone, String website,
        String addressLine1, String addressLine2, String city, String stateRegion, String postalCode, String country,
        String defaultCurrency, String timezone, String locale,
        String taxId, String vatNumber, String transportLicenseNumber,
        Integer maxShipmentsPerMonth, Integer maxUsers, SubscriptionTier subscriptionTier,
        boolean active, boolean suspended, String suspensionReason,
        long locationCount, long activeLocationCount,
        Instant createdAt, Instant updatedAt,
        List<LocationResponse> locations
) {
}
