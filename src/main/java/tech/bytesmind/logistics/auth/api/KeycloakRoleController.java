package tech.bytesmind.logistics.auth.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.bytesmind.logistics.auth.api.dto.KeycloakAssignRoleRequest;
import tech.bytesmind.logistics.auth.api.dto.KeycloakCreateRoleRequest;
import tech.bytesmind.logistics.auth.api.dto.KeycloakRoleResponse;
import tech.bytesmind.logistics.auth.domain.model.Role;
import tech.bytesmind.logistics.auth.application.service.KeycloakRoleService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Keycloak role management.
 * Provides endpoints for role CRUD operations and user role assignment.
 * Implements hybrid approach: roles stored locally, optionally synced to Keycloak.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/keycloak/roles")
@Tag(name = "Keycloak Role Management", description = "Endpoints for managing roles with hybrid Keycloak integration")
@SecurityRequirement(name = "bearer-jwt")
@Slf4j
public class KeycloakRoleController {

    private final KeycloakRoleService keycloakRoleService;

    /**
     * POST /admin/keycloak/roles
     * Creates a new role.
     *
     * @param request the role creation request
     * @return ResponseEntity with created role and 201 status
     */
    @PostMapping
    @Operation(summary = "Create a new role", description = "Creates a new role in the system with optional Keycloak sync")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Role created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or role already exists"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<KeycloakRoleResponse> createRole(@Valid @RequestBody KeycloakCreateRoleRequest request) {
        log.info("Creating role: {}", request.getCode());
        String roleId = keycloakRoleService.createRole(request);

        // Retrieve the created role
        Role role = keycloakRoleService.getRoleById(roleId)
                .orElseThrow(() -> new RuntimeException("Failed to retrieve created role"));

        KeycloakRoleResponse response = toResponse(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /admin/keycloak/roles/{roleId}
     * Retrieves a role by ID.
     *
     * @param roleId the role ID
     * @return ResponseEntity with role details
     */
    @GetMapping("/{roleId}")
    @Operation(summary = "Get role by ID", description = "Retrieves a role by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Role not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<KeycloakRoleResponse> getRole(@PathVariable String roleId) {
        log.debug("Fetching role: {}", roleId);
        Role role = keycloakRoleService.getRoleById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
        return ResponseEntity.ok(toResponse(role));
    }

    /**
     * GET /admin/keycloak/roles/code/{code}
     * Retrieves a role by code.
     *
     * @param code the role code
     * @return ResponseEntity with role details
     */
    @GetMapping("/code/{code}")
    @Operation(summary = "Get role by code", description = "Retrieves a role by its code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Role not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<KeycloakRoleResponse> getRoleByCode(@PathVariable String code) {
        log.debug("Fetching role by code: {}", code);
        Role role = keycloakRoleService.getRoleByCode(code)
                .orElseThrow(() -> new RuntimeException("Role not found: " + code));
        return ResponseEntity.ok(toResponse(role));
    }

    /**
     * GET /admin/keycloak/roles
     * Lists all roles.
     *
     * @return ResponseEntity with list of roles
     */
    @GetMapping
    @Operation(summary = "List all roles", description = "Lists all roles in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Roles retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<List<KeycloakRoleResponse>> listAllRoles() {
        log.debug("Listing all roles");
        List<KeycloakRoleResponse> roles = keycloakRoleService.listAllRoles()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roles);
    }

    /**
     * POST /admin/keycloak/roles/{roleId}/assign
     * Assigns a role to a user.
     *
     * @param roleId the role ID
     * @param request the assignment request
     * @return ResponseEntity with 200 OK status
     */
    @PostMapping("/{roleId}/assign")
    @Operation(summary = "Assign role to user", description = "Assigns a role to a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role assigned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Role or user not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<Void> assignRoleToUser(
            @PathVariable String roleId,
            @Valid @RequestBody KeycloakAssignRoleRequest request) {
        log.info("Assigning role {} to user {}", roleId, request.getKeycloakUserId());
        request.setRoleId(roleId);
        keycloakRoleService.assignRoleToUser(request);
        return ResponseEntity.ok().build();
    }

    /**
     * DELETE /admin/keycloak/roles/{roleId}/users/{userId}
     * Removes a role from a user.
     *
     * @param roleId the role ID
     * @param userId the user ID
     * @return ResponseEntity with 204 No Content status
     */
    @DeleteMapping("/{roleId}/users/{userId}")
    @Operation(summary = "Remove role from user", description = "Removes a role from a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Role removed successfully"),
            @ApiResponse(responseCode = "404", description = "Role or user not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<Void> removeRoleFromUser(
            @PathVariable String roleId,
            @PathVariable String userId) {
        log.info("Removing role {} from user {}", roleId, userId);
        keycloakRoleService.removeRoleFromUser(roleId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /admin/keycloak/roles/{roleId}/users
     * Lists all users with a specific role.
     *
     * @param roleId the role ID
     * @return ResponseEntity with list of user IDs
     */
    @GetMapping("/{roleId}/users")
    @Operation(summary = "List users with role", description = "Lists all users assigned to a specific role")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Role not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<List<String>> listUsersWithRole(@PathVariable String roleId) {
        log.debug("Listing users with role: {}", roleId);
        List<String> userIds = keycloakRoleService.listUsersWithRole(roleId);
        return ResponseEntity.ok(userIds);
    }

    /**
     * PUT /admin/keycloak/roles/{roleId}
     * Updates a role.
     *
     * @param roleId the role ID
     * @param request the update request
     * @return ResponseEntity with updated role
     */
    @PutMapping("/{roleId}")
    @Operation(summary = "Update role", description = "Updates a role's details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Role not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<KeycloakRoleResponse> updateRole(
            @PathVariable String roleId,
            @Valid @RequestBody KeycloakCreateRoleRequest request) {
        log.info("Updating role: {}", roleId);
        Role updated = keycloakRoleService.updateRole(roleId, request);
        return ResponseEntity.ok(toResponse(updated));
    }

    /**
     * DELETE /admin/keycloak/roles/{roleId}
     * Deletes a role.
     *
     * @param roleId the role ID
     * @return ResponseEntity with 204 No Content status
     */
    @DeleteMapping("/{roleId}")
    @Operation(summary = "Delete role", description = "Permanently deletes a role")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Role deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Role not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<Void> deleteRole(@PathVariable String roleId) {
        log.info("Deleting role: {}", roleId);
        keycloakRoleService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Helper method to convert Role entity to response DTO.
     *
     * @param role the role entity
     * @return the response DTO
     */
    private KeycloakRoleResponse toResponse(Role role) {
        return KeycloakRoleResponse.builder()
                .id(role.getId().toString())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .build();
    }
}

