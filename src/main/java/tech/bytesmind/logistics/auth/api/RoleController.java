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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tech.bytesmind.logistics.auth.api.dto.CreateRoleRequest;
import tech.bytesmind.logistics.auth.api.dto.RoleResponse;
import tech.bytesmind.logistics.auth.api.dto.UpdateRoleRequest;
import tech.bytesmind.logistics.auth.application.mapper.RoleMapper;
import tech.bytesmind.logistics.auth.application.service.RoleManagementService;
import tech.bytesmind.logistics.auth.domain.model.RoleScope;

import java.util.List;
import java.util.UUID;

/**
 * Admin REST controller for role management.
 *
 * <p>All mutations require {@code ROLE_PLATFORM_ADMIN}.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/roles")
@Tag(name = "Role Management", description = "Administrative endpoints for managing application roles")
@SecurityRequirement(name = "oauth2")
@Slf4j
public class RoleController {

    private final RoleManagementService roleManagementService;
    private final RoleMapper roleMapper;

    // =========================================================================
    // Create
    // =========================================================================

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create a new role")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Role created"),
            @ApiResponse(responseCode = "400", description = "Invalid request or duplicate code"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        var role = roleManagementService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(roleMapper.toResponse(role));
    }

    // =========================================================================
    // Read
    // =========================================================================

    @GetMapping("/{roleId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'AGENCY_ADMIN')")
    @Operation(summary = "Get role by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role found"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    public ResponseEntity<RoleResponse> getRole(@PathVariable UUID roleId) {
        return ResponseEntity.ok(roleMapper.toResponse(roleManagementService.findById(roleId)));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'AGENCY_ADMIN')")
    @Operation(summary = "Get role by code")
    @ApiResponse(responseCode = "200", description = "Role found")
    public ResponseEntity<RoleResponse> getRoleByCode(@PathVariable String code) {
        return ResponseEntity.ok(roleMapper.toResponse(roleManagementService.findByCode(code)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'AGENCY_ADMIN')")
    @Operation(summary = "List all roles", description = "Optionally filter by scope")
    @ApiResponse(responseCode = "200", description = "Roles returned")
    public ResponseEntity<List<RoleResponse>> listRoles(
            @RequestParam(required = false) RoleScope scope,
            @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        var roles = scope != null
                ? (activeOnly ? roleManagementService.listActiveByScopeAndActive(scope) : roleManagementService.listByScope(scope))
                : (activeOnly ? roleManagementService.listActive() : roleManagementService.listAll());
        return ResponseEntity.ok(roleMapper.toResponseList(roles));
    }

    @GetMapping("/{roleId}/users")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "List user IDs with this role")
    @ApiResponse(responseCode = "200", description = "User IDs returned")
    public ResponseEntity<List<UUID>> listUsersWithRole(@PathVariable UUID roleId) {
        return ResponseEntity.ok(roleManagementService.listUserIdsWithRole(roleId));
    }

    // =========================================================================
    // Update
    // =========================================================================

    @PutMapping("/{roleId}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Update role details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable UUID roleId,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        return ResponseEntity.ok(roleMapper.toResponse(roleManagementService.updateRole(roleId, request)));
    }

    // =========================================================================
    // Activation
    // =========================================================================

    @PutMapping("/{roleId}/activate")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Activate a role")
    @ApiResponse(responseCode = "200", description = "Role activated")
    public ResponseEntity<RoleResponse> activateRole(@PathVariable UUID roleId) {
        return ResponseEntity.ok(roleMapper.toResponse(roleManagementService.activateRole(roleId)));
    }

    @PutMapping("/{roleId}/deactivate")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Deactivate a role")
    @ApiResponse(responseCode = "200", description = "Role deactivated")
    public ResponseEntity<RoleResponse> deactivateRole(@PathVariable UUID roleId) {
        return ResponseEntity.ok(roleMapper.toResponse(roleManagementService.deactivateRole(roleId)));
    }

    // =========================================================================
    // Delete
    // =========================================================================

    @DeleteMapping("/{roleId}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Delete a role")
    @ApiResponse(responseCode = "204", description = "Role deleted")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID roleId) {
        roleManagementService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }
}
