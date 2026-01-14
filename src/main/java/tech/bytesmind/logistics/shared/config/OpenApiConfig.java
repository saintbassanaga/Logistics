package tech.bytesmind.logistics.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration OpenAPI 3 (Swagger).
 * Documentation interactive disponible sur /swagger-ui.html
 * Documentation JSON disponible sur /api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI logisticsOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Local development server"),
                        new Server()
                                .url("https://api.logistics.example.com")
                                .description("Production server")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", securityScheme())
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("Logistics Platform API")
                .description("""
                        **Logistics Platform REST API Documentation**
                        
                        This API provides endpoints for managing logistics operations including:
                        - **Agency Management**: Create and manage logistics agencies
                        - **User Management**: User accounts, roles, and permissions (RBAC/ABAC)
                        - **Parcel Management**: Shipments and parcel tracking
                        
                        ## Authentication
                        All endpoints require JWT Bearer authentication via OAuth2/OIDC (Keycloak).
                        
                        ## Multi-Tenancy
                        The platform enforces strict tenant isolation at 4 levels:
                        1. JWT claims (agency_id)
                        2. ThreadLocal context (TenantContext)
                        3. Repository filtering
                        4. Domain invariants
                        
                        ## Actor Types
                        - **PLATFORM_ADMIN**: Full access to all resources
                        - **AGENCY_EMPLOYEE**: Access scoped to their agency
                        - **CUSTOMER**: Limited access to their own data
                        
                        ## Role Scopes
                        - **PLATFORM**: Cross-agency roles (e.g., PLATFORM_ADMIN)
                        - **AGENCY**: Agency-scoped roles (e.g., AGENCY_ADMIN, SHIPMENT_MANAGER)
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Logistics Platform Team")
                        .email("api@logistics.example.com")
                        .url("https://logistics.example.com"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0"));
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        JWT Bearer token obtained from Keycloak via OAuth2/OIDC.
                        
                        **Token structure:**
                        ```json
                        {
                          "sub": "user-uuid",
                          "email": "user@example.com",
                          "actor_type": "AGENCY_EMPLOYEE",
                          "agency_id": "agency-uuid",
                          "roles": ["AGENCY_ADMIN", "SHIPMENT_MANAGER"]
                        }
                        ```
                        
                        **How to obtain:**
                        1. Direct Password Grant (dev only):
                           ```bash
                           curl -X POST http://localhost:8080/realms/logistics/protocol/openid-connect/token \\
                             -d grant_type=password \\
                             -d client_id=logistics-api \\
                             -d username=user@example.com \\
                             -d password=yourpassword \\
                             -d totp=123456
                           ```
                        
                        2. Authorization Code Flow (production):
                           Use Keycloak's /authorize endpoint
                        """);
    }
}
