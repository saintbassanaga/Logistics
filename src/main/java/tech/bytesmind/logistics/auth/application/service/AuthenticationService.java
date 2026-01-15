package tech.bytesmind.logistics.auth.application.service;

import tech.bytesmind.logistics.auth.api.dto.PasswordUpdateRequest;
import tech.bytesmind.logistics.auth.api.dto.UserProfileUpdateRequest;
import tech.bytesmind.logistics.auth.api.dto.UserRegistrationRequest;
import tech.bytesmind.logistics.auth.domain.model.User;

import java.util.UUID;

/**
 * Interface du service applicatif pour l'authentification et l'inscription.
 * L'authentification JWT est gérée par Keycloak.
 * Ce service gère l'inscription et la gestion du profil utilisateur.
 */
public interface AuthenticationService {

    /**
     * Inscrit un nouvel utilisateur (self-registration).
     * L'utilisateur est créé avec le rôle USER et actorType CUSTOMER.
     * Note: L'utilisateur doit également être créé dans Keycloak pour l'authentification.
     */
    User registerUser(UserRegistrationRequest request);

    /**
     * Met à jour le mot de passe d'un utilisateur.
     * Met à jour le hash dans la base locale.
     * Note: Pour une synchronisation complète, le mot de passe devrait aussi être mis à jour dans Keycloak.
     */
    void updatePassword(UUID userId, PasswordUpdateRequest request);

    /**
     * Met à jour le profil d'un utilisateur (self-service).
     */
    User updateProfile(UUID userId, UserProfileUpdateRequest request);

    /**
     * Récupère le profil de l'utilisateur courant.
     */
    User getCurrentUserProfile(UUID userId);

    /**
     * Vérifie si un email est déjà utilisé.
     */
    boolean isEmailTaken(String email);

    /**
     * Vérifie si un nom d'utilisateur est déjà utilisé.
     */
    boolean isUsernameTaken(String username);

    /**
     * Met à jour le timestamp de dernière connexion.
     * Appelé après une authentification réussie via Keycloak.
     */
    void recordLogin(UUID userId);
}