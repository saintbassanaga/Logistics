package tech.bytesmind.logistics.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tech.bytesmind.logistics.auth.domain.model.RoleScope;

public record CreateRoleRequest(
        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = "^[A-Z_]+$", message = "Role code must be uppercase letters and underscores only")
        String code,

        @NotBlank @Size(max = 255) String name,

        @Size(max = 1000) String description,

        @NotNull RoleScope scope
) {
}
