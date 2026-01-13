package tech.bytesmind.logistics.shared.tenancy.filter;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tech.bytesmind.logistics.shared.security.model.SecurityContext;
import tech.bytesmind.logistics.shared.security.service.SecurityContextService;
import tech.bytesmind.logistics.shared.tenancy.model.TenantContext;

import java.io.IOException;

/**
 * TenantContextFilter is a Spring `OncePerRequestFilter` that handles tenant-specific context
 * management during HTTP request processing. This filter is executed for every request
 * and ensures thread-local tenant information is properly set and cleared.
 * <p>
 * The filter interacts with the `SecurityContextService` to extract tenant-specific
 * details (e.g., `agencyId`) from the current security context and sets this information
 * into the `TenantContext`. The context is automatically cleared at the end of
 * processing to avoid resource leaks or inconsistencies.
 * <p>
 * Responsibilities:
 * - Retrieves the current security context using the `SecurityContextService`.
 * - Sets the `agencyId` into the `TenantContext` when present.
 * - Ensures proper cleanup of the `TenantContext` after the request is processed.
 * <p>
 * Error handling:
 * - The filtering logic is enclosed in a `try-finally` block to guarantee that the
 *   thread-local context is always cleared, even if an exception is thrown during
 *   request processing.
 * <p>
 * Order and component configuration:
 * - Marked with `@Order(2)` to define its position in the filter chain.
 * - Annotated with `@Component` for automatic Spring Bean detection and registration.
 * <p>
 * This filter is useful in multi-tenant applications to maintain tenant isolation
 * and ensure that tenant-specific data is only accessible within the scope of a
 * single request. It relies on the `TenantContext` class for thread-local context storage.
 */
@Component
@Order(2)
public class TenantContextFilter extends OncePerRequestFilter {
    
    private final SecurityContextService securityContextService;
    
    public TenantContextFilter(SecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }
    
    @Override
    protected void doFilterInternal(
        @NonNull  HttpServletRequest request,
        @NonNull  HttpServletResponse response,
        @NonNull  FilterChain filterChain
    ) throws ServletException, IOException {
        
        try {
            SecurityContext securityContext = securityContextService.getCurrentSecurityContext();
            
            if (securityContext.agencyId() != null) {
                TenantContext.setCurrentAgencyId(securityContext.agencyId());
            }
            
            filterChain.doFilter(request, response);
            
        } finally {
            TenantContext.clear();
        }
    }
}