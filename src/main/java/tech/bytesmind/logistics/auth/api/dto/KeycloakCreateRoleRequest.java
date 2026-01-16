package tech.bytesmind.logistics.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a role with hybrid management (Keycloak + Local DB).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeycloakCreateRoleRequest {

    @NotBlank(message = "Role code must not be blank")
    @Size(min = 3, max = 50, message = "Role code must be between 3 and 50 characters")
    private String code;  // e.g., "AGENCY_ADMIN", "CUSTOMER_SERVICE"

    @NotBlank(message = "Role name must not be blank")
    @Size(min = 3, max = 255, message = "Role name must be between 3 and 255 characters")
    private String name;  // e.g., "Agency Administrator"

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private boolean syncToKeycloak = true;  // Whether to also create in Keycloak
}

