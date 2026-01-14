package tech.bytesmind.logistics.auth.application.service;

import tech.bytesmind.logistics.auth.domain.model.User;

import java.util.List;
import java.util.UUID;

/**
 * Interface du service applicatif pour les Users.
 */
public interface UserService {

    /**
     * Crée un nouvel utilisateur.
     */
    User createUser(User user);

    /**
     * Récupère un utilisateur par son ID.
     */
    User getUserById(UUID id);

    /**
     * Récupère un utilisateur par son email.
     */
    User getUserByEmail(String email);

    /**
     * Récupère un utilisateur avec ses rôles.
     */
    User getUserWithRoles(UUID id);

    /**
     * Récupère un utilisateur par son external_auth_id (sub JWT).
     */
    User getUserByExternalAuthId(String externalAuthId);

    /**
     * Liste tous les employés d'une agence.
     */
    List<User> listAgencyEmployees(UUID agencyId);

    /**
     * Liste les employés actifs d'une agence.
     */
    List<User> listActiveAgencyEmployees(UUID agencyId);

    /**
     * Assigne un rôle à un utilisateur.
     */
    void assignRole(UUID userId, UUID roleId);

    /**
     * Révoque un rôle d'un utilisateur.
     */
    void revokeRole(UUID userId, UUID roleId);

    /**
     * Désactive un utilisateur.
     */
    void deactivateUser(UUID userId, String reason);

    /**
     * Réactive un utilisateur.
     */
    void activateUser(UUID userId);

    /**
     * Change l'agence d'un employé.
     */
    void changeUserAgency(UUID userId, UUID newAgencyId);

    /**
     * Met à jour les informations d'un utilisateur.
     */
    User updateUser(UUID userId, User updatedData);
}