package tech.bytesmind.logistics.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank @Size(max = 255) String firstName,
        @NotBlank @Size(max = 255) String lastName,
        @Size(max = 50) String phone,
        @Size(max = 100) String jobTitle,
        @Size(max = 100) String department
) {
}
