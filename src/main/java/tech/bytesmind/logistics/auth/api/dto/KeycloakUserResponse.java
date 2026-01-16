package tech.bytesmind.logistics.auth.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for response after creating a user in Keycloak.
 * Contains the UUID of the newly created user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeycloakUserResponse {

    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private boolean enabled;
    private boolean emailVerified;
}

