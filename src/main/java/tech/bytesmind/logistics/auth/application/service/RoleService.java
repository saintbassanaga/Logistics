package tech.bytesmind.logistics.auth.application.service;

import tech.bytesmind.logistics.auth.api.dto.CreateRoleRequest;
import tech.bytesmind.logistics.auth.api.dto.UpdateRoleRequest;
import tech.bytesmind.logistics.auth.domain.model.Role;
import tech.bytesmind.logistics.auth.domain.model.RoleScope;

import java.util.List;
import java.util.UUID;

/**
 * Interface du service applicatif pour les Roles.
 */
public interface RoleService {

    /**
     * Crée un nouveau rôle.
     */
    Role createRole(CreateRoleRequest request);

    /**
     * Récupère un rôle par son ID.
     */
    Role getRoleById(UUID id);

    /**
     * Récupère un rôle par son code.
     */
    Role getRoleByCode(String code);

    /**
     * Liste tous les rôles.
     */
    List<Role> listAllRoles();

    /**
     * Liste les rôles actifs.
     */
    List<Role> listActiveRoles();

    /**
     * Liste les rôles par scope.
     */
    List<Role> listRolesByScope(RoleScope scope);

    /**
     * Met à jour un rôle.
     */
    Role updateRole(UUID roleId, UpdateRoleRequest request);

    /**
     * Désactive un rôle.
     */
    void deactivateRole(UUID roleId);

    /**
     * Réactive un rôle.
     */
    void activateRole(UUID roleId);
}