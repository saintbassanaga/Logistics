package tech.bytesmind.logistics.shared.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.consent.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.consent.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.util.StringUtils;
import tech.bytesmind.logistics.auth.infrastructure.repository.UserRepository;

import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

/**
 * Spring OAuth2 Authorization Server configuration.
 *
 * <p>Embeds the full OAuth2/OIDC authorization server in this application.
 * Replaces Keycloak as the identity provider.
 *
 * <p>Supported flows:
 * <ul>
 *   <li>Authorization Code + PKCE (Angular SPA, Mobile apps)</li>
 *   <li>Client Credentials (service-to-service)</li>
 *   <li>Refresh Token</li>
 *   <li>OIDC (UserInfo endpoint)</li>
 * </ul>
 *
 * <p>Custom JWT claims added to every access token:
 * <ul>
 *   <li>{@code actor_type} – CUSTOMER, AGENCY_EMPLOYEE, PLATFORM_ADMIN</li>
 *   <li>{@code roles} – list of active role codes</li>
 *   <li>{@code email} – user email</li>
 *   <li>{@code agency_id} – UUID (only for AGENCY_EMPLOYEE)</li>
 * </ul>
 *
 * <p>RSA key management:
 * <ul>
 *   <li>Dev: ephemeral key generated on startup (configure {@code app.security.keystore.path} to disable)</li>
 *   <li>Prod: load from PKCS12 keystore via {@code app.security.keystore.*} properties</li>
 * </ul>
 */
@Configuration
@Slf4j
public class AuthorizationServerConfig {

    @Value("${app.security.issuer:http://localhost:8081}")
    private String issuerUri;

    @Value("${app.security.keystore.path:}")
    private String keystorePath;

    @Value("${app.security.keystore.password:}")
    private String keystorePassword;

    @Value("${app.security.keystore.alias:logistics-signing-key}")
    private String keyAlias;

    // =========================================================================
    // Security Filter Chain — Authorization Server endpoints (@Order 1)
    // =========================================================================

    /**
     * Highest-priority chain: handles all Spring AS protocol endpoints.
     * Redirects unauthenticated HTML browsers to /login.
     * Token endpoint (/oauth2/token) returns 401 for API clients.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();

        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, (authorizationServer) ->
                        authorizationServer.oidc(Customizer.withDefaults())
                )
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                )
                .cors(Customizer.withDefaults());

        return http.build();
    }

    // =========================================================================
    // JDBC Persistence Stores
    // =========================================================================

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    // =========================================================================
    // JWK / JWT
    // =========================================================================

    /**
     * Provides the RSA JWK source used by both the JWT encoder (AS) and decoder (RS).
     * In development, generates an ephemeral key. In production, loads from a PKCS12 keystore.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = loadOrGenerateRsaKey();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    /**
     * Provides the JwtDecoder for the resource server using the same in-memory JWK source.
     * This avoids the chicken-and-egg problem of the RS trying to fetch keys from the AS
     * before the AS is fully started.
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    // =========================================================================
    // Authorization Server Settings
    // =========================================================================

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(issuerUri)
                .build();
    }

    // =========================================================================
    // JWT Token Customizer — enriches access tokens with business claims
    // =========================================================================

    /**
     * Adds custom business claims to every JWT access token and OIDC ID token.
     *
     * <p>For user-authenticated flows (auth code, refresh token):
     * <ul>
     *   <li>Access token: actor_type, roles, email, agency_id</li>
     *   <li>ID token: email, email_verified, given_name, family_name, preferred_username</li>
     * </ul>
     *
     * <p>For client_credentials: no user claims are added.
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer(UserRepository userRepository) {
        return context -> {
            boolean isClientCredentials = AuthorizationGrantType.CLIENT_CREDENTIALS
                    .equals(context.getAuthorizationGrantType());

            if (isClientCredentials) {
                return; // Service-to-service: no user claims
            }

            String principalName = context.getPrincipal().getName();
            UUID userId;
            try {
                userId = UUID.fromString(principalName);
            } catch (IllegalArgumentException e) {
                log.warn("JWT principal '{}' is not a valid user UUID — skipping custom claims", principalName);
                return;
            }

            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                userRepository.findByIdWithRoles(userId).ifPresent(user -> {
                    context.getClaims().claim("actor_type", user.getActorType().name());
                    context.getClaims().claim("roles", user.getRoleCodes());
                    context.getClaims().claim("email", user.getEmail());
                    if (user.getAgencyId() != null) {
                        context.getClaims().claim("agency_id", user.getAgencyId().toString());
                    }
                });
            } else if (OidcScopes.OPENID.equals(context.getTokenType().getValue())
                    || "id_token".equals(context.getTokenType().getValue())) {
                userRepository.findById(userId).ifPresent(user -> {
                    context.getClaims()
                            .claim("email", user.getEmail())
                            .claim("email_verified", user.isEmailVerified())
                            .claim("given_name", user.getFirstName())
                            .claim("family_name", user.getLastName())
                            .claim("preferred_username",
                                    user.getUsername() != null ? user.getUsername() : user.getEmail());
                });
            }
        };
    }

    // =========================================================================
    // Client Seeder
    // =========================================================================

    /**
     * Seeds the three registered OAuth2 clients on application startup
     * (idempotent — skips clients that already exist in the database).
     */
    @Bean
    public OAuth2ClientSeeder oauth2ClientSeeder(
            RegisteredClientRepository clientRepository,
            PasswordEncoder passwordEncoder) {
        return new OAuth2ClientSeeder(clientRepository, passwordEncoder);
    }

    // =========================================================================
    // RSA Key Helpers
    // =========================================================================

    private RSAKey loadOrGenerateRsaKey() {
        if (StringUtils.hasText(keystorePath)) {
            return loadFromKeystore();
        }
        log.warn("No keystore configured — generating ephemeral RSA-2048 key. " +
                "Set app.security.keystore.path for production use.");
        return generateEphemeralRsaKey();
    }

    private RSAKey loadFromKeystore() {
        try {
            Resource resource = new ClassPathResource(keystorePath);
            if (!resource.exists()) {
                resource = new FileSystemResource(keystorePath);
            }
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream is = resource.getInputStream()) {
                keyStore.load(is, keystorePassword.toCharArray());
            }
            RSAKey rsaKey = RSAKey.load(keyStore, keyAlias, keystorePassword.toCharArray());
            log.info("RSA signing key loaded from keystore '{}'", keystorePath);
            return rsaKey;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA key from keystore: " + keystorePath, e);
        }
    }

    private static RSAKey generateEphemeralRsaKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA algorithm not available", e);
        }
    }

    // =========================================================================
    // Inner: Client Seeder
    // =========================================================================

    /**
     * Registers the three OAuth2 clients required by the platform.
     * Runs once on ApplicationReadyEvent; idempotent.
     */
    public static class OAuth2ClientSeeder
            implements org.springframework.context.ApplicationListener<
            org.springframework.boot.context.event.ApplicationReadyEvent> {

        private final RegisteredClientRepository clientRepository;
        private final PasswordEncoder passwordEncoder;

        public OAuth2ClientSeeder(RegisteredClientRepository clientRepository,
                                  PasswordEncoder passwordEncoder) {
            this.clientRepository = clientRepository;
            this.passwordEncoder = passwordEncoder;
        }

        @Override
        public void onApplicationEvent(
                org.springframework.boot.context.event.ApplicationReadyEvent event) {
            seedAngularClient();
            seedMobileClient();
            seedServiceClient();
        }

        /** Angular SPA: public client, Authorization Code + PKCE, 15-min access tokens. */
        private void seedAngularClient() {
            final String clientId = "logistics-angular";
            if (clientRepository.findByClientId(clientId) != null) return;

            RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(clientId)
                    .clientName("Logistics Angular SPA")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .redirectUri("http://localhost:4200/callback")
                    .redirectUri("http://localhost:4200/silent-callback")
                    .redirectUri("http://localhost:8081/swagger-ui/oauth2-redirect.html")
                    .postLogoutRedirectUri("http://localhost:4200")
                    .scope(OidcScopes.OPENID)
                    .scope(OidcScopes.PROFILE)
                    .scope(OidcScopes.EMAIL)
                    .clientSettings(ClientSettings.builder()
                            .requireAuthorizationConsent(false)
                            .requireProofKey(true)  // PKCE mandatory for public clients
                            .build())
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenTimeToLive(Duration.ofMinutes(15))
                            .refreshTokenTimeToLive(Duration.ofDays(30))
                            .reuseRefreshTokens(false)
                            .build())
                    .build();

            clientRepository.save(client);
            log.info("OAuth2 client '{}' registered", clientId);
        }

        /** Mobile app: public client, Authorization Code + PKCE, custom URI scheme. */
        private void seedMobileClient() {
            final String clientId = "logistics-mobile";
            if (clientRepository.findByClientId(clientId) != null) return;

            RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(clientId)
                    .clientName("Logistics Mobile App")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .redirectUri("logistics://callback")
                    .redirectUri("logistics://silent-callback")
                    .redirectUri("http://localhost:8080/callback") // for local testing
                    .postLogoutRedirectUri("logistics://")
                    .scope(OidcScopes.OPENID)
                    .scope(OidcScopes.PROFILE)
                    .scope(OidcScopes.EMAIL)
                    .clientSettings(ClientSettings.builder()
                            .requireAuthorizationConsent(false)
                            .requireProofKey(true)
                            .build())
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenTimeToLive(Duration.ofMinutes(15))
                            .refreshTokenTimeToLive(Duration.ofDays(90))
                            .reuseRefreshTokens(false)
                            .build())
                    .build();

            clientRepository.save(client);
            log.info("OAuth2 client '{}' registered", clientId);
        }

        /** Backend services: confidential client, Client Credentials flow only. */
        private void seedServiceClient() {
            final String clientId = "logistics-service";
            if (clientRepository.findByClientId(clientId) != null) return;

            RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(clientId)
                    .clientSecret(passwordEncoder.encode("logistics-service-secret-change-in-prod"))
                    .clientName("Logistics Backend Services")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .scope("logistics.read")
                    .scope("logistics.write")
                    .clientSettings(ClientSettings.builder()
                            .requireAuthorizationConsent(false)
                            .requireProofKey(false)
                            .build())
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenTimeToLive(Duration.ofHours(1))
                            .build())
                    .build();

            clientRepository.save(client);
            log.info("OAuth2 client '{}' registered", clientId);
        }
    }
}
