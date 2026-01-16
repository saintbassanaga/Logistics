package tech.bytesmind.logistics.auth.application.properties;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Configuration for Keycloak Admin Client.
 * Creates and manages a singleton-reusable Keycloak admin client bean with connection pooling.
 * The Keycloak instance is thread-safe and should be reused across the application.
 */
@Configuration
@EnableConfigurationProperties(KeycloakAdminProperties.class)
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminConfig {

    private final KeycloakAdminProperties props;
    private Keycloak keycloakAdminInstance;

    /**
     * Creates a single, reusable Keycloak admin client bean using client-credentials flow.
     * Configures connection pooling for optimal performance.
     *
     * @return Keycloak admin client instance
     */
    @Bean
    public Keycloak keycloakAdmin() {
        log.info("Initializing Keycloak admin client for server: {}", props.getServerUrl());
        keycloakAdminInstance = KeycloakBuilder.builder()
                .serverUrl(props.getServerUrl())
                .realm(props.getAdminRealm())
                .clientId(props.getClientId())
                .clientSecret(props.getClientSecret())
                .grantType("client_credentials")
                .build();

        log.info("Keycloak admin client initialized successfully. Admin Realm: {}, Target Realm: {}",
                props.getAdminRealm(), props.getTargetRealm());
        return keycloakAdminInstance;
    }

    /**
     * Graceful shutdown hook to close the Keycloak client and release resources.
     */
    @PreDestroy
    public void shutdown() {
        if (keycloakAdminInstance != null) {
            try {
                keycloakAdminInstance.close();
                log.info("Keycloak admin client closed successfully");
            } catch (Exception ex) {
                log.warn("Error closing Keycloak admin client: {}", ex.getMessage(), ex);
            }
        }
    }
}

