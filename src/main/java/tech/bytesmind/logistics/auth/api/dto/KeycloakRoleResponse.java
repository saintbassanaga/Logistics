package tech.bytesmind.logistics.auth.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for role response with hybrid management data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeycloakRoleResponse {

    private String id;
    private String code;
    private String name;
    private String description;
    private boolean syncedToKeycloak;
    private long userCount;  // Number of users with this role
}

