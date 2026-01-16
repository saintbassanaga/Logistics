package tech.bytesmind.logistics.shared.tenancy.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tech.bytesmind.logistics.shared.security.model.SecurityContext;
import tech.bytesmind.logistics.shared.security.service.SecurityContextService;
import tech.bytesmind.logistics.shared.tenancy.model.TenantContext;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantContextFilter extends OncePerRequestFilter {

    private final SecurityContextService securityContextService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof Jwt jwt) {
                try {
                    SecurityContext securityContext = securityContextService.getCurrentSecurityContext();

                    if (securityContext.agencyId() != null) {
                        TenantContext.setCurrentAgencyId(securityContext.agencyId());
                    }
                } catch (Exception e) {
                    log.warn("Could not set TenantContext: {}", e.getMessage());
                }
            }

            filterChain.doFilter(request, response);

        } finally {
            TenantContext.clear();
        }
    }
}
