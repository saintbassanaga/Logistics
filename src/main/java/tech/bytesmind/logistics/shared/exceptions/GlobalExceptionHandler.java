package tech.bytesmind.logistics.shared.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * A global exception handler to manage custom exceptions and generic errors across the application.
 * This class utilizes {@link RestControllerAdvice} to intercept exceptions raised by controllers
 * and map them to appropriate HTTP responses.
 * <p>
 * It logs the exception details and returns a {@code ProblemDetail} to provide
 * a consistent structure for error responses.
 * <p>
 * The following exception types are handled:
 * - {@link SecurityViolationException}: Indicates a security violation and maps to HTTP 403 Forbidden.
 * - {@link TenantViolationException}: Indicates a multi-tenant violation and maps to HTTP 403 Forbidden.
 * - {@link BusinessException}: Represents business logic-related errors and maps to HTTP 400 Bad Request.
 * - {@link Exception}: Handles all other unexpected exceptions and maps to HTTP 500 Internal Server Error.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(SecurityViolationException.class)
    public ProblemDetail handleSecurityViolation(SecurityViolationException ex) {
        log.error("Security violation: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            ex.getMessage()
        );
        problem.setTitle("Security Violation");
        return problem;
    }
    
    @ExceptionHandler(TenantViolationException.class)
    public ProblemDetail handleTenantViolation(TenantViolationException ex) {
        log.error("Tenant violation: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            ex.getMessage()
        );
        problem.setTitle("Tenant Violation");
        return problem;
    }
    
    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        problem.setTitle("Business Rule Violation");
        return problem;
    }
    
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred"
        );
        problem.setTitle("Internal Server Error");
        return problem;
    }
}