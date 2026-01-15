package tech.bytesmind.logistics.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO pour l'inscription d'un nouvel utilisateur (self-registration).
 * Par défaut, l'utilisateur sera créé avec le rôle USER et actorType CUSTOMER.
 */
public record UserRegistrationRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, underscores and hyphens")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                message = "Password must contain at least one lowercase, one uppercase, one digit and one special character"
        )
        String password,

        @NotBlank(message = "First name is required")
        @Size(max = 255, message = "First name must not exceed 255 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 255, message = "Last name must not exceed 255 characters")
        String lastName,

        @Size(max = 50, message = "Phone must not exceed 50 characters")
        String phone
) {
}