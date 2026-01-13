package tech.bytesmind.logistics.shared.exceptions;

/**
 * Exception levée en cas de violation de sécurité.
 * Conforme à ADR-010 : Zero Trust Monolith.
 */
public class SecurityViolationException extends RuntimeException {
    public SecurityViolationException(String message) {
        super(message);
    }
}