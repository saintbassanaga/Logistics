package tech.bytesmind.logistics.agency.api.dto;

import jakarta.validation.constraints.*;
import tech.bytesmind.logistics.agency.domain.model.LocationType;

import java.math.BigDecimal;

public record CreateLocationRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 255) String name,
        @NotNull LocationType locationType,
        @NotBlank @Size(max = 255) String addressLine1,
        @Size(max = 255) String addressLine2,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 100) String stateRegion,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Size(min = 2, max = 2) String country,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 50) String phone,
        @Size(max = 255) String contactPersonName,
        @Size(max = 50) String contactPersonPhone,
        String openingHours,
        @NotNull Boolean isPickupPoint,
        @NotNull Boolean isDeliveryPoint,
        @NotNull Boolean isWarehouse,
        @Positive Integer maxDailyParcels,
        @Positive BigDecimal storageCapacityM3
) {
}
