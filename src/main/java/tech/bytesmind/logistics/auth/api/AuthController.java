package tech.bytesmind.logistics.auth.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.bytesmind.logistics.auth.api.dto.UserRegistrationRequest;
import tech.bytesmind.logistics.auth.api.dto.UserRegistrationResponse;
import tech.bytesmind.logistics.auth.application.service.RegistrationService;

import java.util.Map;

/**
 * Public authentication endpoints.
 *
 * <p>All paths under {@code /auth} are permit-all (no token required).
 * Token issuance itself is handled by the embedded Spring Authorization Server
 * at {@code /oauth2/token} and {@code /oauth2/authorize}.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Public registration and availability check endpoints")
@Slf4j
public class AuthController {

    private final RegistrationService registrationService;

    /**
     * POST /auth/register
     * Self-registration for new customers. Creates a CUSTOMER account with the USER role.
     * Email verification is required before the account can be used for login.
     */
    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new CUSTOMER account. Email verification is required before login."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registration successful"),
            @ApiResponse(responseCode = "400", description = "Validation error or duplicate email/username"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<UserRegistrationResponse> register(
            @Valid @RequestBody UserRegistrationRequest request
    ) {
        UserRegistrationResponse response = registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /auth/check-email?email=...
     * Returns whether an email address is available for registration.
     */
    @GetMapping("/check-email")
    @Operation(summary = "Check email availability")
    @ApiResponse(responseCode = "200", description = "{\"available\": true|false}")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam String email) {
        return ResponseEntity.ok(Map.of("available", registrationService.isEmailAvailable(email)));
    }

    /**
     * GET /auth/check-username?username=...
     * Returns whether a username is available for registration.
     */
    @GetMapping("/check-username")
    @Operation(summary = "Check username availability")
    @ApiResponse(responseCode = "200", description = "{\"available\": true|false}")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        return ResponseEntity.ok(Map.of("available", registrationService.isUsernameAvailable(username)));
    }
}
