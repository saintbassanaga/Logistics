package tech.bytesmind.logistics.shared.exceptions;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Centralised exception handling for all REST controllers.
 * Maps domain and framework exceptions to RFC 7807 ProblemDetail responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final URI SECURITY_VIOLATION_TYPE  = URI.create("https://logistics.example.com/errors/security-violation");
    private static final URI TENANT_VIOLATION_TYPE    = URI.create("https://logistics.example.com/errors/tenant-violation");
    private static final URI BUSINESS_RULE_TYPE       = URI.create("https://logistics.example.com/errors/business-rule-violation");
    private static final URI VALIDATION_TYPE          = URI.create("https://logistics.example.com/errors/validation");
    private static final URI INTERNAL_ERROR_TYPE      = URI.create("https://logistics.example.com/errors/internal-server-error");

    // =========================================================================
    // Security
    // =========================================================================

    @ExceptionHandler(SecurityViolationException.class)
    public ProblemDetail handleSecurityViolation(SecurityViolationException ex) {
        log.warn("Security violation: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Security Violation");
        problem.setType(SECURITY_VIOLATION_TYPE);
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
        problem.setTitle("Access Denied");
        problem.setType(SECURITY_VIOLATION_TYPE);
        return problem;
    }

    // =========================================================================
    // Multi-tenancy
    // =========================================================================

    @ExceptionHandler(TenantViolationException.class)
    public ProblemDetail handleTenantViolation(TenantViolationException ex) {
        log.warn("Tenant violation: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Tenant Violation");
        problem.setType(TENANT_VIOLATION_TYPE);
        return problem;
    }

    // =========================================================================
    // Business rules
    // =========================================================================

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex) {
        log.debug("Business rule violation: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Business Rule Violation");
        problem.setType(BUSINESS_RULE_TYPE);
        return problem;
    }

    // =========================================================================
    // Validation
    // =========================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.debug("Validation failed: {}", detail);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation Error");
        problem.setType(VALIDATION_TYPE);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.debug("Constraint violation: {}", detail);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation Error");
        problem.setType(VALIDATION_TYPE);
        return problem;
    }

    // =========================================================================
    // Auth
    // =========================================================================

    @ExceptionHandler(UsernameNotFoundException.class)
    public ProblemDetail handleUsernameNotFound(UsernameNotFoundException ex) {
        // Do not reveal whether the user exists or not
        log.debug("Username not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        problem.setTitle("Authentication Failed");
        return problem;
    }

    // =========================================================================
    // Fallback
    // =========================================================================

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        problem.setType(INTERNAL_ERROR_TYPE);
        return problem;
    }
}
