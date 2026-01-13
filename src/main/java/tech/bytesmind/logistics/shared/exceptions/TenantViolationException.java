package tech.bytesmind.logistics.shared.exceptions;

/**
 * Exception levée en cas de violation multi-tenant.
 * Conforme à ADR-006.
 */
public class TenantViolationException extends RuntimeException {
    public TenantViolationException(String message) {
        super(message);
    }
}