package tech.bytesmind.logistics.auth.application.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Keycloak Admin Client configuration properties.
 * Binds to keycloak.admin.* properties from application.yml
 */
@Data
@Validated
@ConfigurationProperties(prefix = "keycloak.admin")
public class KeycloakAdminProperties {

    /**
     * Keycloak server base URL (e.g., http://localhost:8080)
     */
    @NotBlank(message = "Keycloak server URL must not be blank")
    private String serverUrl;

    /**
     * Admin realm (typically 'master')
     */
    @NotBlank(message = "Keycloak admin realm must not be blank")
    private String adminRealm = "master";

    /**
     * Service account client ID for admin API
     */
    @NotBlank(message = "Keycloak admin client ID must not be blank")
    private String clientId;

    /**
     * Service account client secret
     */
    @NotBlank(message = "Keycloak admin client secret must not be blank")
    private String clientSecret;

    /**
     * Target realm where users will be managed
     */
    @NotBlank(message = "Keycloak target realm must not be blank")
    private String targetRealm;

    /**
     * HTTP connection pool size for Keycloak admin client
     */
    private int connectionPoolSize = 10;
}

