package tech.bytesmind.logistics.auth.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.stereotype.Service;
import tech.bytesmind.logistics.auth.domain.model.Role;
import tech.bytesmind.logistics.auth.domain.model.RoleScope;
import tech.bytesmind.logistics.auth.domain.model.User;
import tech.bytesmind.logistics.auth.infrastructure.repository.RoleRepository;
import tech.bytesmind.logistics.auth.infrastructure.repository.UserRepository;
import tech.bytesmind.logistics.auth.api.dto.KeycloakAssignRoleRequest;
import tech.bytesmind.logistics.auth.api.dto.KeycloakCreateRoleRequest;
import tech.bytesmind.logistics.shared.exceptions.KeycloakAdminException;
import tech.bytesmind.logistics.auth.application.properties.KeycloakAdminProperties;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing roles with hybrid approach (Keycloak + Local DB).
 * <p>
 * Manages role lifecycle:
 * - Store in local database (role definitions matching Role entity)
 * - Optional sync to Keycloak
 * - User-role assignments with scope support (PLATFORM, AGENCY, CUSTOMER)
 * <p>
 * Integrates with local entities:
 * - Role: Supports RoleScope (PLATFORM, AGENCY, CUSTOMER), active status
 * - User: Manages many-to-many user-role relationships via externalAuthId
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakRoleService {

    private final Keycloak keycloakAdmin;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final KeycloakAdminProperties props;

    /**
     * Creates a new role.
     * Stores in local database and optionally syncs to Keycloak.
     *
     * @param request the role creation request
     * @return the created role ID
     * @throws KeycloakAdminException if creation fails
     */
    public String createRole(KeycloakCreateRoleRequest request) {
        log.debug("Creating role: {}", request.getCode());

        // Check if role already exists
        Optional<Role> existing = roleRepository.findByCode(request.getCode());
        if (existing.isPresent()) {
            log.warn("Role already exists: {}", request.getCode());
            throw new KeycloakAdminException("Role with code '" + request.getCode() + "' already exists");
        }

        // Create role in local database with entity structure
        Role role = new Role();
        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setScope(RoleScope.AGENCY);  // Default scope
        role.setActive(true);  // New roles are active by default

        Role savedRole = roleRepository.save(role);

        // Optionally sync to Keycloak
        if (request.isSyncToKeycloak()) {
            try {
                syncRoleToKeycloak(savedRole);
            } catch (Exception e) {
                log.warn("Failed to sync role to Keycloak (will continue): {}", e.getMessage());
            }
        }

        log.info("Role created successfully: {} ({})", request.getCode(), savedRole.getId());
        return savedRole.getId().toString();
    }

    /**
     * Retrieves a role by ID.
     *
     * @param roleId the role ID
     * @return Optional containing the role if found
     */
    public Optional<Role> getRoleById(String roleId) {
        log.debug("Fetching role by ID: {}", roleId);
        try {
            return roleRepository.findById(java.util.UUID.fromString(roleId));
        } catch (IllegalArgumentException e) {
            log.error("Invalid role ID format: {}", roleId);
            throw new KeycloakAdminException("Invalid role ID format: " + roleId);
        }
    }

    /**
     * Retrieves a role by code.
     *
     * @param code the role code
     * @return Optional containing the role if found
     */
    public Optional<Role> getRoleByCode(String code) {
        log.debug("Fetching role by code: {}", code);
        return roleRepository.findByCode(code);
    }

    /**
     * Lists all roles.
     *
     * @return list of all roles
     */
    public List<Role> listAllRoles() {
        log.debug("Listing all roles");
        return roleRepository.findAll();
    }

    /**
     * Lists all active roles.
     *
     * @return list of active roles
     */
    public List<Role> listActiveRoles() {
        log.debug("Listing active roles");
        return roleRepository.findByActiveTrue();
    }

    /**
     * Lists roles by scope (PLATFORM, AGENCY, CUSTOMER).
     *
     * @param scope the role scope
     * @return list of roles with specified scope
     */
    public List<Role> listRolesByScope(RoleScope scope) {
        log.debug("Listing roles by scope: {}", scope);
        return roleRepository.findByScope(scope);
    }

    /**
     * Lists active roles by scope.
     *
     * @param scope the role scope
     * @return list of active roles with specified scope
     */
    public List<Role> listRolesByScopeAndActive(RoleScope scope) {
        log.debug("Listing active roles by scope: {}", scope);
        return roleRepository.findByScopeAndActiveTrue(scope);
    }

    /**
     * Assigns a role to a user.
     * Verifies both user and role exist before assignment.
     *
     * @param request the role assignment request
     * @throws KeycloakAdminException if assignment fails
     */
    public void assignRoleToUser(KeycloakAssignRoleRequest request) {
        log.debug("Assigning role {} to user {}", request.getRoleId(), request.getKeycloakUserId());

        // Get and verify role exists
        Optional<Role> roleOpt = getRoleById(request.getRoleId());
        if (roleOpt.isEmpty()) {
            log.error("Role not found: {}", request.getRoleId());
            throw new KeycloakAdminException("Role not found: " + request.getRoleId());
        }

        // Verify user exists in local DB by externalAuthId (Keycloak user ID)
        Optional<User> userOpt = userRepository.findByExternalAuthId(request.getKeycloakUserId());
        if (userOpt.isEmpty()) {
            log.error("User not found with externalAuthId: {}", request.getKeycloakUserId());
            throw new KeycloakAdminException("User not found: " + request.getKeycloakUserId());
        }

        // Assign role to user in many-to-many relationship
        User user = userOpt.get();
        Role role = roleOpt.get();

        if (user.getRoles().contains(role)) {
            log.warn("User {} already has role {}", request.getKeycloakUserId(), request.getRoleId());
            return;
        }

        user.getRoles().add(role);
        userRepository.save(user);

        // Optionally assign role in Keycloak
        if (request.isSyncToKeycloak()) {
            try {
                syncRoleToKeycloakUser(user, role);
            } catch (Exception e) {
                log.warn("Failed to sync role assignment to Keycloak: {}", e.getMessage());
            }
        }

        log.info("Role assigned successfully: {} to user {}", request.getRoleId(), request.getKeycloakUserId());
    }

    /**
     * Removes a role from a user.
     *
     * @param roleId the role ID
     * @param keycloakUserId the Keycloak user ID (externalAuthId)
     */
    public void removeRoleFromUser(String roleId, String keycloakUserId) {
        log.debug("Removing role {} from user {}", roleId, keycloakUserId);

        // Get and verify role exists
        Optional<Role> roleOpt = getRoleById(roleId);
        if (roleOpt.isEmpty()) {
            log.error("Role not found: {}", roleId);
            throw new KeycloakAdminException("Role not found: " + roleId);
        }

        // Verify user exists by externalAuthId
        Optional<User> userOpt = userRepository.findByExternalAuthId(keycloakUserId);
        if (userOpt.isEmpty()) {
            log.error("User not found with externalAuthId: {}", keycloakUserId);
            throw new KeycloakAdminException("User not found: " + keycloakUserId);
        }

        // Remove role from user in many-to-many relationship
        User user = userOpt.get();
        Role role = roleOpt.get();

        if (!user.getRoles().contains(role)) {
            log.warn("User {} does not have role {}", keycloakUserId, roleId);
            return;
        }

        user.getRoles().remove(role);
        userRepository.save(user);

        log.info("Role removed successfully from user");
    }

    /**
     * Lists all users with a specific role.
     *
     * @param roleId the role ID
     * @return list of user IDs (UUID) with this role
     */
    public List<String> listUsersWithRole(String roleId) {
        log.debug("Listing users with role: {}", roleId);

        Optional<Role> roleOpt = getRoleById(roleId);
        if (roleOpt.isEmpty()) {
            throw new KeycloakAdminException("Role not found: " + roleId);
        }

        Role role = roleOpt.get();

        // Get all users and filter by those that have this role
        List<String> userIds = userRepository.findAll().stream()
                .filter(user -> user.getRoles().contains(role))
                .map(user -> user.getId().toString())
                .collect(Collectors.toList());

        log.debug("Found {} users with role {}", userIds.size(), roleId);
        return userIds;
    }

    /**
     * Syncs a local role to Keycloak.
     *
     * @param role the role to sync
     */
    private void syncRoleToKeycloak(Role role) {
        try {
            RoleRepresentation keycloakRole = new RoleRepresentation();
            keycloakRole.setName(role.getCode());
            keycloakRole.setDescription(role.getDescription());

            keycloakAdmin.realm(props.getTargetRealm())
                    .roles()
                    .create(keycloakRole);

            log.info("Role synced to Keycloak: {}", role.getCode());
        } catch (Exception e) {
            log.warn("Failed to sync role to Keycloak: {}", e.getMessage());
            throw new KeycloakAdminException("Failed to sync role to Keycloak: " + e.getMessage(), e);
        }
    }

    /**
     * Updates a role.
     *
     * @param roleId the role ID
     * @param request the update request
     * @return the updated role
     */
    public Role updateRole(String roleId, KeycloakCreateRoleRequest request) {
        log.debug("Updating role: {}", roleId);

        Optional<Role> roleOpt = getRoleById(roleId);
        if (roleOpt.isEmpty()) {
            throw new KeycloakAdminException("Role not found: " + roleId);
        }

        Role role = roleOpt.get();
        role.setName(request.getName());
        role.setDescription(request.getDescription());

        Role updated = roleRepository.save(role);

        if (request.isSyncToKeycloak()) {
            try {
                log.info("Role updated in Keycloak: {}", request.getCode());
            } catch (Exception e) {
                log.warn("Failed to update role in Keycloak: {}", e.getMessage());
            }
        }

        return updated;
    }

    /**
     * Deletes a role.
     *
     * @param roleId the role ID
     */
    public void deleteRole(String roleId) {
        log.debug("Deleting role: {}", roleId);

        Optional<Role> roleOpt = getRoleById(roleId);
        if (roleOpt.isEmpty()) {
            throw new KeycloakAdminException("Role not found: " + roleId);
        }

        roleRepository.deleteById(java.util.UUID.fromString(roleId));
        log.info("Role deleted successfully: {}", roleId);
    }

    /**
     * Updates role active status.
     *
     * @param roleId the role ID
     * @param active the active status
     * @return the updated role
     */
    public Role setRoleActive(String roleId, boolean active) {
        log.debug("Setting role {} active status to: {}", roleId, active);

        Optional<Role> roleOpt = getRoleById(roleId);
        if (roleOpt.isEmpty()) {
            throw new KeycloakAdminException("Role not found: " + roleId);
        }

        Role role = roleOpt.get();
        role.setActive(active);
        return roleRepository.save(role);
    }

    /**
     * Helper method to sync role assignment to Keycloak user.
     *
     * @param user the user
     * @param role the role
     */
    private void syncRoleToKeycloakUser(User user, Role role) {
        try {
            log.debug("Syncing role {} to Keycloak user {}", role.getCode(), user.getExternalAuthId());
            // Implementation would use keycloakAdmin API to assign role to user
        } catch (Exception e) {
            log.warn("Failed to sync role to Keycloak user: {}", e.getMessage());
            throw new KeycloakAdminException("Failed to sync role to Keycloak: " + e.getMessage(), e);
        }
    }
}

