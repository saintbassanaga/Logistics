package tech.bytesmind.logistics.auth.api;

import io.swagger.v3.oas.annotations.Operation;
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
import tech.bytesmind.logistics.auth.api.dto.*;
import tech.bytesmind.logistics.auth.application.mapper.UserMapper;
import tech.bytesmind.logistics.auth.application.service.AuthenticationService;
import tech.bytesmind.logistics.auth.domain.model.User;
import tech.bytesmind.logistics.shared.security.service.SecurityContextService;

import java.util.Map;

/**
 * REST Controller pour l'authentification et la gestion du profil utilisateur.
 * L'authentification JWT est gérée par Keycloak.
 * Ce controller gère l'inscription et les opérations de profil.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User registration and profile management endpoints")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationService authenticationService;
    private final UserMapper userMapper;
    private final SecurityContextService securityContextService;

    /**
     * POST /auth/register
     * Inscrit un nouvel utilisateur (self-registration).
     * Endpoint public - pas d'authentification requise.
     */
    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with CUSTOMER role. The user will need to authenticate via Keycloak after registration."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = UserRegistrationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email or username already exists", content = @Content)
    })
    public ResponseEntity<UserRegistrationResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        log.info("POST /auth/register - Registering user: {}", request.email());

        User user = authenticationService.registerUser(request);

        UserRegistrationResponse response = new UserRegistrationResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getActorType().name(),
                user.getRoleCodes(),
                user.isActive(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /auth/check-email
     * Vérifie si un email est déjà utilisé.
     * Endpoint public - pas d'authentification requise.
     */
    @GetMapping("/check-email")
    @Operation(
            summary = "Check if email is available",
            description = "Returns whether the specified email is already registered."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Check completed",
                    content = @Content(schema = @Schema(implementation = Map.class)))
    })
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam String email) {
        log.debug("GET /auth/check-email - Checking email: {}", email);

        boolean taken = authenticationService.isEmailTaken(email);
        return ResponseEntity.ok(Map.of("taken", taken, "available", !taken));
    }

    /**
     * GET /auth/check-username
     * Vérifie si un nom d'utilisateur est déjà utilisé.
     * Endpoint public - pas d'authentification requise.
     */
    @GetMapping("/check-username")
    @Operation(
            summary = "Check if username is available",
            description = "Returns whether the specified username is already registered."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Check completed",
                    content = @Content(schema = @Schema(implementation = Map.class)))
    })
    public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        log.debug("GET /auth/check-username - Checking username: {}", username);

        boolean taken = authenticationService.isUsernameTaken(username);
        return ResponseEntity.ok(Map.of("taken", taken, "available", !taken));
    }

    /**
     * GET /auth/profile
     * Récupère le profil de l'utilisateur connecté.
     * Nécessite authentification.
     */
    @GetMapping("/profile")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
            summary = "Get current user profile",
            description = "Retrieves the profile of the currently authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    public ResponseEntity<UserResponse> getProfile() {
        log.info("GET /auth/profile");

        var context = securityContextService.getCurrentSecurityContext();
        User user = authenticationService.getCurrentUserProfile(context.userId());

        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    /**
     * PUT /auth/profile
     * Met à jour le profil de l'utilisateur connecté.
     * Nécessite authentification.
     */
    @PutMapping("/profile")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
            summary = "Update current user profile",
            description = "Updates the profile of the currently authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        log.info("PUT /auth/profile");

        var context = securityContextService.getCurrentSecurityContext();
        User user = authenticationService.updateProfile(context.userId(), request);

        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    /**
     * PUT /auth/password
     * Met à jour le mot de passe de l'utilisateur connecté.
     * Nécessite authentification.
     */
    @PutMapping("/password")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
            summary = "Update password",
            description = "Updates the password of the currently authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or incorrect current password", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody PasswordUpdateRequest request) {
        log.info("PUT /auth/password");

        var context = securityContextService.getCurrentSecurityContext();
        authenticationService.updatePassword(context.userId(), request);

        return ResponseEntity.noContent().build();
    }
}