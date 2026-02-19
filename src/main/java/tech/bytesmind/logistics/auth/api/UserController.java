package tech.bytesmind.logistics.auth.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tech.bytesmind.logistics.auth.api.dto.CreateUserRequest;
import tech.bytesmind.logistics.auth.api.dto.UpdateUserRequest;
import tech.bytesmind.logistics.auth.api.dto.UserResponse;
import tech.bytesmind.logistics.auth.application.mapper.UserMapper;
import tech.bytesmind.logistics.auth.application.service.UserManagementService;

import java.util.List;
import java.util.UUID;

/**
 * Admin REST controller for platform user management.
 *
 * <p>Requires {@code ROLE_PLATFORM_ADMIN} for all write operations.
 * Read operations require at minimum {@code ROLE_AGENCY_ADMIN}.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/users")
@Tag(name = "User Management", description = "Administrative endpoints for managing platform users")
@SecurityRequirement(name = "oauth2")
@Slf4j
public class UserController {

    private final UserManagementService userManagementService;
    private final UserMapper userMapper;

    // =========================================================================
    // Create
    // =========================================================================

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create a user", description = "Admin creates a user with optional initial password")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(responseCode = "400", description = "Invalid request or duplicate email/username"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @RequestParam(required = false) String initialPassword
    ) {
        var user = userManagementService.createUser(request, initialPassword);
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toResponse(user));
    }

    // =========================================================================
    // Current user profile (accessible to any authenticated user)
    // =========================================================================

    /**
     * GET /admin/users/me
     * Returns the profile of the currently authenticated user.
     * Accessible to any valid JWT holder — no role restriction.
     */
    @GetMapping("/me")
    @Operation(summary = "Get own profile", description = "Returns the authenticated user's own profile")
    @ApiResponse(responseCode = "200", description = "Profile returned")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(userMapper.toResponse(userManagementService.findById(userId)));
    }

    // =========================================================================
    // Read
    // =========================================================================

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'AGENCY_ADMIN')")
    @Operation(summary = "Get user by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userMapper.toResponse(userManagementService.findById(userId)));
    }

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "List all users")
    @ApiResponse(responseCode = "200", description = "Users returned")
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(userMapper.toResponseList(userManagementService.listAll()));
    }

    @GetMapping("/by-agency/{agencyId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'AGENCY_ADMIN')")
    @Operation(summary = "List users by agency")
    @ApiResponse(responseCode = "200", description = "Users returned")
    public ResponseEntity<List<UserResponse>> listUsersByAgency(@PathVariable UUID agencyId) {
        return ResponseEntity.ok(userMapper.toResponseList(userManagementService.listByAgency(agencyId)));
    }

    // =========================================================================
    // Update
    // =========================================================================

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Update user details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        var user = userManagementService.updateUser(userId, request);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    // =========================================================================
    // Activation / Deactivation
    // =========================================================================

    @PutMapping("/{userId}/activate")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Activate user account")
    @ApiResponse(responseCode = "200", description = "User activated")
    public ResponseEntity<UserResponse> activateUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userMapper.toResponse(userManagementService.activateUser(userId)));
    }

    @PutMapping("/{userId}/deactivate")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Deactivate user account")
    @ApiResponse(responseCode = "200", description = "User deactivated")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userMapper.toResponse(userManagementService.deactivateUser(userId)));
    }

    // =========================================================================
    // Email verification
    // =========================================================================

    @PostMapping("/{userId}/verify-email")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Mark email as verified (admin override)")
    @ApiResponse(responseCode = "200", description = "Email marked as verified")
    public ResponseEntity<UserResponse> verifyEmail(@PathVariable UUID userId) {
        return ResponseEntity.ok(userMapper.toResponse(userManagementService.verifyEmail(userId)));
    }

    // =========================================================================
    // Password management
    // =========================================================================

    @PostMapping("/{userId}/reset-password")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Admin password reset")
    @ApiResponse(responseCode = "204", description = "Password reset successfully")
    public ResponseEntity<Void> resetPassword(
            @PathVariable UUID userId,
            @RequestParam @NotBlank @Size(min = 8, max = 100) String newPassword
    ) {
        userManagementService.resetPassword(userId, newPassword);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Role assignment
    // =========================================================================

    @PostMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Assign role to user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role assigned"),
            @ApiResponse(responseCode = "400", description = "Role scope mismatch or role already assigned")
    })
    public ResponseEntity<UserResponse> assignRole(
            @PathVariable UUID userId,
            @PathVariable UUID roleId
    ) {
        return ResponseEntity.ok(userMapper.toResponse(userManagementService.assignRole(userId, roleId)));
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Revoke role from user")
    @ApiResponse(responseCode = "200", description = "Role revoked")
    public ResponseEntity<UserResponse> revokeRole(
            @PathVariable UUID userId,
            @PathVariable UUID roleId
    ) {
        return ResponseEntity.ok(userMapper.toResponse(userManagementService.revokeRole(userId, roleId)));
    }

    // =========================================================================
    // Delete
    // =========================================================================

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Soft-delete a user")
    @ApiResponse(responseCode = "204", description = "User deleted")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userManagementService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
