package tech.bytesmind.logistics.auth.application.mapper;

import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;
import tech.bytesmind.logistics.auth.api.dto.KeycloakCreateUserRequest;
import tech.bytesmind.logistics.auth.api.dto.KeycloakUserResponse;

/**
 * Mapper for Keycloak user representations and DTOs.
 * Converts between request/response DTOs and Keycloak representations.
 */
@Component
public class KeycloakUserMapper {

    /**
     * Maps a create user request to Keycloak UserRepresentation.
     *
     * @param request the create user request
     * @return UserRepresentation for Keycloak API
     */
    public UserRepresentation toKeycloakUser(KeycloakCreateUserRequest request) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUsername());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setEnabled(request.isEnabled());
        user.setEmailVerified(request.isEmailVerified());
        return user;
    }

    /**
     * Maps a Keycloak UserRepresentation to a response DTO.
     *
     * @param user the Keycloak user representation
     * @return response DTO
     */
    public KeycloakUserResponse toResponse(UserRepresentation user) {
        return KeycloakUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .emailVerified(user.isEmailVerified())
                .build();
    }
}
