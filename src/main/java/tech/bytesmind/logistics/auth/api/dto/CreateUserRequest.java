package tech.bytesmind.logistics.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tech.bytesmind.logistics.shared.security.model.ActorType;

import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(max = 255) String firstName,
        @NotBlank @Size(max = 255) String lastName,
        @Size(max = 50) String phone,
        @NotNull ActorType actorType,
        UUID agencyId,
        @Size(max = 100) String jobTitle,
        @Size(max = 100) String department
) {
}
