package tech.bytesmind.logistics.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for assigning a role to a user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeycloakAssignRoleRequest {

    @NotBlank(message = "Role ID must not be blank")
    private String roleId;

    @NotBlank(message = "User ID (Keycloak) must not be blank")
    private String keycloakUserId;

    private boolean syncToKeycloak = true;  // Whether to also assign in Keycloak
}

