package tech.bytesmind.logistics.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public OpenAPI logisticsOpenAPI() {
        // Base URLs for OIDC endpoints derived from issuer-uri
        String authUrl = issuerUri + "/protocol/openid-connect/auth";
        String tokenUrl = issuerUri + "/protocol/openid-connect/token";

        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Local development server"),
                        new Server().url("https://api.logistics.example.com").description("Production server")
                ))
                .addSecurityItem(new SecurityRequirement().addList("keycloak-oauth"))
                .components(new Components()
                        .addSecuritySchemes("keycloak-oauth", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .description("Keycloak OpenID Connect Authentication")
                                .flows(new OAuthFlows()
                                        .authorizationCode(new OAuthFlow()
                                                .authorizationUrl(authUrl)
                                                .tokenUrl(tokenUrl)
                                        )
                                )
                        )
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("Logistics Platform API")
                .description("""
                        **Logistics Platform REST API Documentation**
                        
                        ## Authentication (2026 Standards)
                        This API is secured via **Keycloak**. 
                        - Click the **Authorize** button below.
                        - Use Client ID: `logistics-frontend` (Public) or `logistics-backend` (with secret).
                        - **PKCE** is enabled and handled automatically by Swagger UI.
                        
                        ## Multi-Tenancy
                        Strict isolation via `agency_id` claim in JWT.
                        """)
                .version("1.0.0")
                .contact(new Contact().name("Logistics Platform Team").email("api@logistics.example.com"))
                .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0"));
    }
}
