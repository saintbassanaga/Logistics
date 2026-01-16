package tech.bytesmind.logistics.shared.error.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import tech.bytesmind.logistics.shared.exceptions.KeycloakAdminException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler for Keycloak admin operations.
 * Maps KeycloakAdminException to appropriate HTTP responses.
 */
@RestControllerAdvice
@Slf4j
public class KeycloakExceptionHandler {

    /**
     * Handles KeycloakAdminException.
     * Returns 400 Bad Request for user already exists errors.
     * Returns 500 Internal Server Error for other failures.
     *
     * @param ex the KeycloakAdminException
     * @param request the web request
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(KeycloakAdminException.class)
    public ResponseEntity<Map<String, Object>> handleKeycloakAdminException(
            KeycloakAdminException ex,
            WebRequest request
    ) {
        log.error("Keycloak admin operation failed: {}", ex.getMessage(), ex);

        HttpStatus status = ex.getMessage().contains("already exists")
                ? HttpStatus.CONFLICT
                : HttpStatus.INTERNAL_SERVER_ERROR;

        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(errorResponse, status);
    }
}

