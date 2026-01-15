package tech.bytesmind.logistics.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO pour la demande de connexion (authentification locale).
 * L'identifiant peut être un username ou un email.
 */
public record LoginRequest(
        @NotBlank(message = "Identifier (username or email) is required")
        String identifier,

        @NotBlank(message = "Password is required")
        String password
) {
}