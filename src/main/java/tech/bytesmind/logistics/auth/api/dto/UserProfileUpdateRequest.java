package tech.bytesmind.logistics.auth.api.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO pour la mise à jour du profil utilisateur (self-service).
 * Permet à l'utilisateur de mettre à jour ses informations personnelles.
 * Ne permet pas de modifier: email, username, actorType, agencyId, roles.
 */
public record UserProfileUpdateRequest(
        @Size(max = 255, message = "First name must not exceed 255 characters")
        String firstName,

        @Size(max = 255, message = "Last name must not exceed 255 characters")
        String lastName,

        @Size(max = 50, message = "Phone must not exceed 50 characters")
        String phone
) {
}