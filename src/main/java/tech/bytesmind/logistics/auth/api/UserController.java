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
import tech.bytesmind.logistics.auth.api.dto.AssignRoleRequest;
import tech.bytesmind.logistics.auth.api.dto.CreateUserRequest;
import tech.bytesmind.logistics.auth.api.dto.UpdateUserRequest;
import tech.bytesmind.logistics.auth.api.dto.UserResponse;
import tech.bytesmind.logistics.auth.application.mapper.UserMapper;
import tech.bytesmind.logistics.auth.application.service.UserService;
import tech.bytesmind.logistics.auth.domain.model.User;
import tech.bytesmind.logistics.auth.application.policy.UserAccessPolicy;
import tech.bytesmind.logistics.shared.security.annotations.RequireRole;
import tech.bytesmind.logistics.shared.security.service.SecurityContextService;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller pour la gestion des utilisateurs.
 * Endpoints sécurisés avec RBAC (annotations @RequireRole) et ABAC (UserAccessPolicy).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Tag(name = "User Management", description = "Endpoints for managing platform users with RBAC/ABAC enforcement")
@SecurityRequirement(name = "bearer-jwt")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final UserMapper userMapper;
    private final SecurityContextService securityContextService;
    private final UserAccessPolicy userAccessPolicy;

    /**
     * POST /users
     * Créer un nouvel utilisateur.
     * Accessible par: PLATFORM_ADMIN (tous types), AGENCY_ADMIN (AGENCY_EMPLOYEE seulement).
     */
    @PostMapping
    @Operation(
            summary = "Create a new user",
            description = "Creates a new platform user. PLATFORM_ADMIN can create all user types. AGENCY_ADMIN can only create AGENCY_EMPLOYEE users for their agency."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "409", description = "User with email already exists", content = @Content)
    })
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("POST /users - Creating user: {}", request.email());

        var context = securityContextService.getCurrentSecurityContext();
        if (!userAccessPolicy.canCreate(context, request.actorType())) {
            log.warn("User creation denied for actor {} creating {}",
                    context.actorType(), request.actorType());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User user = userService.createUser(request);
        UserResponse response = userMapper.toResponse(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /users/{id}
     * Récupérer un utilisateur par ID.
     * Accessible par: PLATFORM_ADMIN, propriétaire, ou collègues d'agence.
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get user by ID",
            description = "Retrieves a user by their ID. PLATFORM_ADMIN can view all users. AGENCY_EMPLOYEE can view colleagues. Users can view their own profile."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<UserResponse> getUser(
            @Parameter(description = "User ID", required = true) @PathVariable UUID id) {
        log.info("GET /users/{}", id);

        User user = userService.getUserById(id);

        var context = securityContextService.getCurrentSecurityContext();
        userAccessPolicy.validateAccess(context, user); // Validation ABAC

        UserResponse response = userMapper.toResponse(user);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /users/{id}
     * Mettre à jour les informations d'un utilisateur.
     * Accessible par: PLATFORM_ADMIN, AGENCY_ADMIN (pour ses employés), ou propriétaire.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        log.info("PUT /users/{}", id);

        User user = userService.getUserById(id);

        var context = securityContextService.getCurrentSecurityContext();
        userAccessPolicy.validateModify(context, user); // Validation ABAC

        User updatedUser = userService.updateUser(id, request);
        UserResponse response = userMapper.toResponse(updatedUser);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /users/{id}/assign-role
     * Assigner un rôle à un utilisateur.
     * Accessible par: PLATFORM_ADMIN, AGENCY_ADMIN (rôles AGENCY-scoped uniquement).
     */
    @PostMapping("/{id}/assign-role")
    @RequireRole("AGENCY_ADMIN")
    public ResponseEntity<UserResponse> assignRole(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRoleRequest request
    ) {
        log.info("POST /users/{}/assign-role - roleId: {}", id, request.roleId());

        User user = userService.getUserById(id);

        var context = securityContextService.getCurrentSecurityContext();
        userAccessPolicy.validateAssignRoles(context, user); // Validation ABAC

        User updatedUser = userService.assignRole(id, request.roleId());
        UserResponse response = userMapper.toResponse(updatedUser);

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /users/{id}/remove-role/{roleId}
     * Retirer un rôle d'un utilisateur.
     * Accessible par: PLATFORM_ADMIN, AGENCY_ADMIN (rôles AGENCY-scoped uniquement).
     */
    @DeleteMapping("/{id}/remove-role/{roleId}")
    @RequireRole("AGENCY_ADMIN")
    public ResponseEntity<UserResponse> removeRole(
            @PathVariable UUID id,
            @PathVariable UUID roleId
    ) {
        log.info("DELETE /users/{}/remove-role/{}", id, roleId);

        User user = userService.getUserById(id);

        var context = securityContextService.getCurrentSecurityContext();
        userAccessPolicy.validateAssignRoles(context, user); // Validation ABAC

        User updatedUser = userService.removeRole(id, roleId);
        UserResponse response = userMapper.toResponse(updatedUser);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /users/{id}/deactivate
     * Désactiver un utilisateur.
     * Accessible par: PLATFORM_ADMIN, AGENCY_ADMIN (pour ses employés).
     */
    @PostMapping("/{id}/deactivate")
    @RequireRole("AGENCY_ADMIN")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID id) {
        log.info("POST /users/{}/deactivate", id);

        User user = userService.getUserById(id);

        var context = securityContextService.getCurrentSecurityContext();

        if (!userAccessPolicy.canDeactivate(context, user)) {
            log.warn("User deactivation denied for user {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /users/{id}/activate
     * Réactiver un utilisateur désactivé.
     * Accessible par: PLATFORM_ADMIN, AGENCY_ADMIN (pour ses employés).
     */
    @PostMapping("/{id}/activate")
    @RequireRole("AGENCY_ADMIN")
    public ResponseEntity<Void> activateUser(@PathVariable UUID id) {
        log.info("POST /users/{}/activate", id);

        User user = userService.getUserById(id);

        var context = securityContextService.getCurrentSecurityContext();
        userAccessPolicy.validateModify(context, user); // Validation ABAC

        userService.activateUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /users/agency/{agencyId}
     * Lister les utilisateurs d'une agence.
     * Accessible par: PLATFORM_ADMIN, employés de l'agence.
     */
    @GetMapping("/agency/{agencyId}")
    public ResponseEntity<List<UserResponse>> listUsersByAgency(@PathVariable UUID agencyId) {
        log.info("GET /users/agency/{}", agencyId);

        var context = securityContextService.getCurrentSecurityContext();

        if (!userAccessPolicy.canListAgencyUsers(context, agencyId)) {
            log.warn("Listing agency users denied for agency {}", agencyId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<User> users = userService.listUsersByAgency(agencyId);
        List<UserResponse> responses = userMapper.toResponseList(users);

        return ResponseEntity.ok(responses);
    }

    /**
     * GET /users/me
     * Récupérer le profil de l'utilisateur connecté.
     */
    @GetMapping("/me")
    @Operation(
            summary = "Get current user profile",
            description = "Retrieves the profile of the currently authenticated user based on JWT claims."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    public ResponseEntity<UserResponse> getCurrentUser() {
        log.info("GET /users/me");

        var context = securityContextService.getCurrentSecurityContext();
        User user = userService.getUserById(context.userId());

        UserResponse response = userMapper.toResponse(user);
        return ResponseEntity.ok(response);
    }
}
