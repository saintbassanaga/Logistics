package tech.bytesmind.logistics.parcel.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateParcelRequest(
        @NotNull @Positive BigDecimal weight,
        @Positive BigDecimal length,
        @Positive BigDecimal width,
        @Positive BigDecimal height,
        @NotBlank String description,
        BigDecimal declaredValue,
        @Size(min = 3, max = 3) String currency,
        @Size(max = 255) String specificReceiverName,
        @Size(max = 50) String specificReceiverPhone,
        @Size(max = 500) String specificReceiverAddress,
        String notes
) {
}
