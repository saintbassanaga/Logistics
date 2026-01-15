package tech.bytesmind.logistics.auth.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.bytesmind.logistics.auth.api.dto.CreateRoleRequest;
import tech.bytesmind.logistics.auth.api.dto.RoleResponse;
import tech.bytesmind.logistics.auth.api.dto.UpdateRoleRequest;
import tech.bytesmind.logistics.auth.application.mapper.RoleMapper;
import tech.bytesmind.logistics.auth.application.service.RoleService;
import tech.bytesmind.logistics.auth.domain.model.Role;
import tech.bytesmind.logistics.auth.application.policy.RoleAccessPolicy;
import tech.bytesmind.logistics.shared.security.annotations.RequireActor;
import tech.bytesmind.logistics.shared.security.model.ActorType;
import tech.bytesmind.logistics.shared.security.service.SecurityContextService;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller pour la gestion des rôles.
 * Endpoints sécurisés avec RBAC (annotations @RequireActor) et ABAC (RoleAccessPolicy).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/roles")
@Tag(name = "Role Management", description = "Endpoints for managing roles and permissions. PLATFORM-scoped and AGENCY-scoped roles.")
@SecurityRequirement(name = "bearer-jwt")
public class RoleController {

    private static final Logger log = LoggerFactory.getLogger(RoleController.class);

    private final RoleService roleService;
    private final RoleMapper roleMapper;
    private final SecurityContextService securityContextService;
    private final RoleAccessPolicy roleAccessPolicy;

    /**
     * POST /roles
     * Créer un nouveau rôle.
     * Accessible par: PLATFORM_ADMIN uniquement.
     */
    @PostMapping
    @RequireActor(ActorType.PLATFORM_ADMIN)
    @Operation(
            summary = "Create a new role",
            description = "Creates a new role with PLATFORM or AGENCY scope. Only PLATFORM_ADMIN can create roles."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Role created successfully",
                    content = @Content(schema = @Schema(implementation = RoleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions (not PLATFORM_ADMIN)", content = @Content),
            @ApiResponse(responseCode = "409", description = "Role with code already exists", content = @Content)
    })
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        log.info("POST /roles - Creating role: {}", request.code());

        var context = securityContextService.getCurrentSecurityContext();

        if (!roleAccessPolicy.canCreate(context)) {
            log.warn("Role creation denied");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Role role = roleService.createRole(request);
        RoleResponse response = roleMapper.toResponse(role);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /roles
     * Lister tous les rôles.
     * Accessible par tous les utilisateurs authentifiés.
     */
    @GetMapping
    @Operation(
            summary = "List all roles",
            description = "Retrieves all roles (both PLATFORM and AGENCY scoped). Available to all authenticated users."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    public ResponseEntity<List<RoleResponse>> listRoles() {
        log.info("GET /roles - Listing all roles");

        List<Role> roles = roleService.listAllRoles();
        List<RoleResponse> responses = roleMapper.toResponseList(roles);

        return ResponseEntity.ok(responses);
    }

    /**
     * GET /roles/active
     * Lister les rôles actifs.
     * Accessible par tous les utilisateurs authentifiés.
     */
    @GetMapping("/active")
    public ResponseEntity<List<RoleResponse>> listActiveRoles() {
        log.info("GET /roles/active - Listing active roles");

        List<Role> roles = roleService.listActiveRoles();
        List<RoleResponse> responses = roleMapper.toResponseList(roles);

        return ResponseEntity.ok(responses);
    }

    /**
     * GET /roles/{id}
     * Récupérer un rôle par ID.
     * Accessible par tous les utilisateurs authentifiés.
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get role by ID",
            description = "Retrieves a role by its UUID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RoleResponse.class))),
            @ApiResponse(responseCode = "404", description = "Role not found", content = @Content)
    })
    public ResponseEntity<RoleResponse> getRole(
            @Parameter(description = "Role ID", required = true) @PathVariable UUID id) {
        log.info("GET /roles/{}", id);

        Role role = roleService.getRoleById(id);
        RoleResponse response = roleMapper.toResponse(role);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /roles/code/{code}
     * Récupérer un rôle par son code.
     * Accessible par tous les utilisateurs authentifiés.
     */
    @GetMapping("/code/{code}")
    @Operation(
            summary = "Get role by code",
            description = "Retrieves a role by its code (e.g., 'AGENCY_ADMIN', 'SHIPMENT_MANAGER')."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RoleResponse.class))),
            @ApiResponse(responseCode = "404", description = "Role not found", content = @Content)
    })
    public ResponseEntity<RoleResponse> getRoleByCode(
            @Parameter(description = "Role code (e.g., 'AGENCY_ADMIN')", required = true) @PathVariable String code) {
        log.info("GET /roles/code/{}", code);

        Role role = roleService.getRoleByCode(code);
        RoleResponse response = roleMapper.toResponse(role);

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /roles/{id}
     * Mettre à jour un rôle.
     * Accessible par: PLATFORM_ADMIN uniquement.
     */
    @PutMapping("/{id}")
    @RequireActor(ActorType.PLATFORM_ADMIN)
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        log.info("PUT /roles/{}", id);

        Role role = roleService.getRoleById(id);

        var context = securityContextService.getCurrentSecurityContext();

        if (!roleAccessPolicy.canModify(context, role)) {
            log.warn("Role modification denied for role {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Role updatedRole = roleService.updateRole(id, request);
        RoleResponse response = roleMapper.toResponse(updatedRole);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /roles/{id}/deactivate
     * Désactiver un rôle.
     * Accessible par: PLATFORM_ADMIN uniquement.
     */
    @PostMapping("/{id}/deactivate")
    @RequireActor(ActorType.PLATFORM_ADMIN)
    public ResponseEntity<Void> deactivateRole(@PathVariable UUID id) {
        log.info("POST /roles/{}/deactivate", id);

        Role role = roleService.getRoleById(id);

        var context = securityContextService.getCurrentSecurityContext();

        if (!roleAccessPolicy.canModify(context, role)) {
            log.warn("Role deactivation denied for role {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        roleService.deactivateRole(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /roles/{id}/activate
     * Réactiver un rôle désactivé.
     * Accessible par: PLATFORM_ADMIN uniquement.
     */
    @PostMapping("/{id}/activate")
    @RequireActor(ActorType.PLATFORM_ADMIN)
    public ResponseEntity<Void> activateRole(@PathVariable UUID id) {
        log.info("POST /roles/{}/activate", id);

        Role role = roleService.getRoleById(id);

        var context = securityContextService.getCurrentSecurityContext();

        if (!roleAccessPolicy.canModify(context, role)) {
            log.warn("Role activation denied for role {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        roleService.activateRole(id);
        return ResponseEntity.noContent().build();
    }
}
