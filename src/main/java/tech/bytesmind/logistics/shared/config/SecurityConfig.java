package tech.bytesmind.logistics.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tech.bytesmind.logistics.shared.security.service.SecurityContextService;
import tech.bytesmind.logistics.shared.tenancy.filter.TenantContextFilter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main application security configuration — Resource Server + Form Login chain.
 *
 * <p>Three-chain architecture (with {@link AuthorizationServerConfig}):
 * <ol>
 *   <li>{@code @Order(1)} — Spring AS protocol endpoints (in {@link AuthorizationServerConfig})</li>
 *   <li>{@code @Order(2)} — This chain: API endpoints (JWT RS) + browser OAuth login (form login)</li>
 * </ol>
 *
 * <p>Session management:
 * <ul>
 *   <li>API requests authenticated with a {@code Bearer} JWT use no session.</li>
 *   <li>Browser requests during the Authorization Code flow create a transient session
 *       (required so Spring Security can remember the user between the login form
 *       submission and the redirect back to the AS authorization endpoint).</li>
 * </ul>
 *
 * <p>JWT claims expected by this resource server:
 * <ul>
 *   <li>{@code sub} — user UUID (principal name)</li>
 *   <li>{@code roles} — list of role code strings</li>
 *   <li>{@code actor_type}, {@code agency_id}, {@code email} — custom business claims</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health",
            "/actuator/info",
            "/error",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/api-docs/**",
            "/auth/register",
            "/auth/check-email",
            "/auth/check-username",
            "/login"
    };

    // =========================================================================
    // Security Filter Chain — API + Form Login (@Order 2)
    // =========================================================================

    /**
     * Default chain handling all requests not matched by the AS chain (@Order 1).
     *
     * <ul>
     *   <li>CSRF: disabled for API and auth paths; enabled for the form login page.</li>
     *   <li>Sessions: {@code IF_REQUIRED} — Spring creates a session for form-login flows,
     *       but not for stateless JWT API calls.</li>
     *   <li>JSON clients get HTTP 401 for unauthenticated requests.</li>
     *   <li>Browser clients are redirected to /login.</li>
     * </ul>
     */
    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            TenantContextFilter tenantContextFilter
    ) throws Exception {

        http
                // CSRF: disabled for stateless API paths; enabled for the HTML login form
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/api/**",
                                "/admin/**",
                                "/auth/**",
                                "/actuator/**",
                                "/oauth2/**"
                        )
                )
                // Sessions: needed for form-login during OAuth authorization code flow
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated()
                )
                // Form login — used by the browser during Authorization Code flow
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                )
                // JWT Resource Server — validates Bearer tokens for API calls
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                // Exception handling: 401 for JSON/API, redirect to /login for browsers
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new MediaTypeRequestMatcher(MediaType.APPLICATION_JSON)
                        )
                )
                // Tenant context populated after JWT is validated
                .addFilterAfter(tenantContextFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    // =========================================================================
    // JWT Converter — simplified for our own Authorization Server
    // =========================================================================

    /**
     * Converts a JWT issued by our embedded Authorization Server into a
     * Spring Security {@link JwtAuthenticationToken}.
     *
     * <p>Our JWTs have:
     * <ul>
     *   <li>{@code sub} — user UUID (principal name)</li>
     *   <li>{@code roles} — flat list of role code strings (no Keycloak nesting)</li>
     * </ul>
     */
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            String principalName = jwt.getSubject();
            Collection<GrantedAuthority> authorities = extractRoleAuthorities(jwt);
            return new JwtAuthenticationToken(jwt, authorities, principalName);
        };
    }

    /**
     * Extracts roles from the flat {@code roles} claim and maps them to
     * Spring Security {@code ROLE_<CODE>} authorities.
     */
    private Collection<GrantedAuthority> extractRoleAuthorities(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null || roles.isEmpty()) {
            return Collections.emptySet();
        }
        return roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
                .collect(Collectors.toUnmodifiableSet());
    }

    // =========================================================================
    // CORS
    // =========================================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:4200",               // Angular dev server
                "http://localhost:8080",               // Swagger UI / tools
                "https://*.logistics.example.com"      // Production wildcard
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // =========================================================================
    // Shared beans
    // =========================================================================

    @Bean
    public TenantContextFilter tenantContextFilter(SecurityContextService securityContextService) {
        return new TenantContextFilter(securityContextService);
    }

    /**
     * BCrypt with cost factor 12 — strong enough for production, acceptable latency.
     * Used by Spring Security's {@code DaoAuthenticationProvider} and by the registration service.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
