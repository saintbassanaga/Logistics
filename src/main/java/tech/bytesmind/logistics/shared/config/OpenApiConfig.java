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

/**
 * OpenAPI / Swagger UI configuration.
 *
 * <p>OAuth2 flows point to the embedded Spring Authorization Server endpoints:
 * <ul>
 *   <li>Authorization: {@code {issuer}/oauth2/authorize}</li>
 *   <li>Token: {@code {issuer}/oauth2/token}</li>
 * </ul>
 *
 * <p>PKCE is enabled for the Swagger UI client ({@code logistics-angular}).
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.security.issuer:http://localhost:8081}")
    private String issuerUri;

    @Bean
    public OpenAPI logisticsOpenAPI() {
        // Spring Authorization Server endpoint paths
        String authUrl  = issuerUri + "/oauth2/authorize";
        String tokenUrl = issuerUri + "/oauth2/token";

        io.swagger.v3.oas.models.security.Scopes scopes =
                new io.swagger.v3.oas.models.security.Scopes()
                        .addString("openid",  "OpenID Connect login")
                        .addString("profile", "User profile information")
                        .addString("email",   "User email address");

        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Local development"),
                        new Server().url("https://api.logistics.example.com").description("Production")
                ))
                .addSecurityItem(new SecurityRequirement().addList("oauth2"))
                .components(new Components()
                        .addSecuritySchemes("oauth2", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .description("Spring Authorization Server — OAuth2 / OIDC")
                                .flows(new OAuthFlows()
                                        .authorizationCode(new OAuthFlow()
                                                .authorizationUrl(authUrl)
                                                .tokenUrl(tokenUrl)
                                                .scopes(scopes)
                                        )
                                )
                        )
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("Logistics Platform API")
                .description("""
                        **Logistics Platform REST API**

                        ## Authentication
                        Secured via the embedded **Spring OAuth2 Authorization Server**.

                        Click **Authorize**, select scopes (`openid profile email`),
                        and use client ID `logistics-angular` (PKCE is handled automatically by Swagger UI).

                        ## Multi-Tenancy
                        Strict tenant isolation via the `agency_id` JWT claim.
                        """)
                .version("2.0.0")
                .contact(new Contact()
                        .name("Logistics Platform Team")
                        .email("api@logistics.example.com"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0"));
    }
}
