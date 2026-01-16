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
import tech.bytesmind.logistics.auth.api.dto.KeycloakCreateUserRequest;
import tech.bytesmind.logistics.auth.api.dto.KeycloakUserResponse;
import tech.bytesmind.logistics.auth.application.mapper.KeycloakUserMapper;
import tech.bytesmind.logistics.auth.application.service.KeycloakUserService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Keycloak user management operations.
 * Provides endpoints to create, read, update, and delete users in Keycloak programmatically.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/keycloak/users")
@Tag(name = "Keycloak User Management", description = "Endpoints for managing Keycloak users programmatically")
@SecurityRequirement(name = "bearer-jwt")
@Slf4j
public class KeycloakUserController {

    private final KeycloakUserService keycloakUserService;
    private final KeycloakUserMapper mapper;

    /**
     * POST /admin/keycloak/users
     * Creates a new user in Keycloak.
     *
     * @param request the user creation request
     * @return ResponseEntity with created user details and 201 status
     */
    @PostMapping
    @Operation(summary = "Create a new user in Keycloak", description = "Creates a new user with username, email, and password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or user already exists"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<KeycloakUserResponse> createUser(@Valid @RequestBody KeycloakCreateUserRequest request) {
        log.info("Creating new user: {}", request.getUsername());
        String userId = keycloakUserService.createUser(request);

        // Retrieve the created user
        var user = keycloakUserService.findById(userId)
                .orElseThrow(() -> new RuntimeException("Failed to retrieve created user"));

        KeycloakUserResponse response = mapper.toResponse(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /admin/keycloak/users/{userId}
     * Retrieves a user by their UUID.
     *
     * @param userId the user's UUID
     * @return ResponseEntity with user details
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Retrieves a user's details by their Keycloak UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<KeycloakUserResponse> getUser(@PathVariable String userId) {
        log.debug("Fetching user: {}", userId);
        var user = keycloakUserService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return ResponseEntity.ok(mapper.toResponse(user));
    }

    /**
     * GET /admin/keycloak/users/search?username={username}
     * Searches for a user by username.
     *
     * @param username the username to search for
     * @return ResponseEntity with matching user(s)
     */
    @GetMapping("/search")
    @Operation(summary = "Search users by username", description = "Searches for users by exact or partial username match")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<List<KeycloakUserResponse>> searchUsers(
            @RequestParam String username,
            @RequestParam(defaultValue = "0") int firstResult,
            @RequestParam(defaultValue = "10") int maxResults
    ) {
        log.debug("Searching for users: {}", username);
        List<KeycloakUserResponse> users = keycloakUserService.searchUsers(username, firstResult, maxResults)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * GET /admin/keycloak/users
     * Lists all users with pagination support.
     *
     * @param firstResult the index of the first result (default: 0)
     * @param maxResults the maximum number of results (default: 10)
     * @return ResponseEntity with list of users
     */
    @GetMapping
    @Operation(summary = "List all users", description = "Lists all users in the realm with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<List<KeycloakUserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int firstResult,
            @RequestParam(defaultValue = "10") int maxResults
    ) {
        log.debug("Listing users (offset: {}, limit: {})", firstResult, maxResults);
        List<KeycloakUserResponse> users = keycloakUserService.listUsers(firstResult, maxResults)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * DELETE /admin/keycloak/users/{userId}
     * Deletes a user from Keycloak.
     *
     * @param userId the user's UUID
     * @return ResponseEntity with 204 No Content status
     */
    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete a user", description = "Permanently deletes a user from Keycloak")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        log.info("Deleting user: {}", userId);
        keycloakUserService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /admin/keycloak/users/{userId}/enable
     * Enables a user account.
     *
     * @param userId the user's UUID
     * @return ResponseEntity with 200 OK status
     */
    @PutMapping("/{userId}/enable")
    @Operation(summary = "Enable a user", description = "Enables a user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User enabled"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<KeycloakUserResponse> enableUser(@PathVariable String userId) {
        log.info("Enabling user: {}", userId);
        keycloakUserService.setUserEnabled(userId, true);
        var user = keycloakUserService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return ResponseEntity.ok(mapper.toResponse(user));
    }

    /**
     * PUT /admin/keycloak/users/{userId}/disable
     * Disables a user account.
     *
     * @param userId the user's UUID
     * @return ResponseEntity with 200 OK status
     */
    @PutMapping("/{userId}/disable")
    @Operation(summary = "Disable a user", description = "Disables a user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User disabled"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<KeycloakUserResponse> disableUser(@PathVariable String userId) {
        log.info("Disabling user: {}", userId);
        keycloakUserService.setUserEnabled(userId, false);
        var user = keycloakUserService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return ResponseEntity.ok(mapper.toResponse(user));
    }

    /**
     * POST /admin/keycloak/users/{userId}/reset-password
     * Resets a user's password.
     *
     * @param userId the user's UUID
     * @param newPassword the new password
     * @return ResponseEntity with 200 OK status
     */
    @PostMapping("/{userId}/reset-password")
    @Operation(summary = "Reset user password", description = "Resets a user's password to a new value")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<Void> resetPassword(
            @PathVariable String userId,
            @RequestParam String newPassword
    ) {
        log.info("Resetting password for user: {}", userId);
        keycloakUserService.setPassword(userId, newPassword, false);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /admin/keycloak/users/{userId}/verify-email
     * Marks a user's email as verified.
     *
     * @param userId the user's UUID
     * @return ResponseEntity with 200 OK status
     */
    @PostMapping("/{userId}/verify-email")
    @Operation(summary = "Verify user email", description = "Marks a user's email as verified")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email verified"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<KeycloakUserResponse> verifyEmail(@PathVariable String userId) {
        log.info("Verifying email for user: {}", userId);
        keycloakUserService.verifyEmail(userId);
        var user = keycloakUserService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return ResponseEntity.ok(mapper.toResponse(user));
    }
}

