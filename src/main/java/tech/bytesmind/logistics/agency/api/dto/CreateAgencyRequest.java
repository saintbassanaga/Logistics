package tech.bytesmind.logistics.agency.api.dto;

import jakarta.validation.constraints.*;
import tech.bytesmind.logistics.agency.domain.model.SubscriptionTier;

public record CreateAgencyRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String legalName,
        @NotBlank @Email String email,
        @Size(max = 50) String phone,
        @Size(max = 255) String website,
        @NotBlank @Size(max = 255) String addressLine1,
        @Size(max = 255) String addressLine2,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 100) String stateRegion,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Size(min = 2, max = 2) String country,
        @NotBlank @Size(min = 3, max = 3) String defaultCurrency,
        @NotBlank @Size(max = 50) String timezone,
        @NotBlank @Size(max = 10) String locale,
        @Size(max = 50) String taxId,
        @Size(max = 50) String vatNumber,
        @Size(max = 100) String transportLicenseNumber,
        @Positive Integer maxShipmentsPerMonth,
        @Positive Integer maxUsers,
        @NotNull SubscriptionTier subscriptionTier
) {
}
