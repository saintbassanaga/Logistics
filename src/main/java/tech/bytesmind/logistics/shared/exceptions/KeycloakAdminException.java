package tech.bytesmind.logistics.shared.exceptions;

/**
 * Runtime exception thrown when Keycloak admin operations fail.
 * Wraps Keycloak/REST API errors into a domain-appropriate exception for cleaner error handling.
 */
public class KeycloakAdminException extends RuntimeException {

    public KeycloakAdminException(String message) {
        super(message);
    }

    public KeycloakAdminException(String message, Throwable cause) {
        super(message, cause);
    }
}

