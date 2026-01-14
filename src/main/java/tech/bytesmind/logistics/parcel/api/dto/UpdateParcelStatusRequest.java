package tech.bytesmind.logistics.parcel.api.dto;

import jakarta.validation.constraints.NotNull;
import tech.bytesmind.logistics.parcel.domain.model.ParcelStatus;

import java.util.UUID;

public record UpdateParcelStatusRequest(
        @NotNull ParcelStatus newStatus,
        UUID locationId
) {
}
