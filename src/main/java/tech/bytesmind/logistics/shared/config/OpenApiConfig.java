package tech.bytesmind.logistics.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import lombok.Data;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Enterprise Logistics Orchestration OpenAPI Specification.
 * Implements OIDC Discovery, Global RFC-7807 Error Handlers, and Multi-Tenant Server Routing.
 */
@Configuration
public class OpenApiConfig {

    private static final String OIDC_SCHEME = "openIdConnect";
    private static final String BEARER_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI logisticsOpenAPI(OpenApiSettings settings) {
        return new OpenAPI()
                .info(createApiInfo(settings))
                .servers(configureServers(settings))
                .addSecurityItem(new SecurityRequirement()
                        .addList(OIDC_SCHEME)
                        .addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(OIDC_SCHEME, createOidcScheme(settings))
                        .addSecuritySchemes(BEARER_SCHEME, createJwtScheme())
                        // Pre-defining global reusable schemas
                        .addSchemas("ProblemDetail", new Schema<>()
                                .type("object")
                                .description("RFC-7807 Compliant Error Response")
                                .properties(Map.of(
                                        "status", new Schema<>().type("integer"),
                                        "title", new Schema<>().type("string"),
                                        "detail", new Schema<>().type("string"),
                                        "instance", new Schema<>().type("string")
                                ))));
    }

    // =========================================================================
    // DOMAIN-DRIVEN API GROUPS
    // =========================================================================

    @Bean
    public GroupedOpenApi platformAdminApi() {
        return createGroup("1-platform-admin", "Platform Admin", "/admin/**", "/agencies/*/status");
    }

    @Bean
    public GroupedOpenApi agencyApi() {
        return GroupedOpenApi.builder()
                .group("2-agency-operations")
                .pathsToMatch("/shipments/**", "/parcels/**", "/agencies/**")
                .pathsToExclude("/admin/**", "/customer/**", "/agencies/register")
                .addOpenApiCustomizer(globalResponseCustomizer())
                .build();
    }

    // =========================================================================
    // SECURITY & PROTOCOL FACTORIES
    // =========================================================================

    private SecurityScheme createOidcScheme(OpenApiSettings settings) {
        return new SecurityScheme()
                .type(SecurityScheme.Type.OPENIDCONNECT)
                .openIdConnectUrl(settings.getIssuerUri() + "/.well-known/openid-configuration")
                .description("OIDC Discovery for Identity Providers (Okta/Keycloak)");
    }

    private SecurityScheme createJwtScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
    }

    // =========================================================================
    // ENHANCED CUSTOMIZERS (RFC-7807 Compliance)
    // =========================================================================

    @Bean
    public OpenApiCustomizer globalResponseCustomizer() {
        return openApi -> openApi.getPaths().values().forEach(pathItem ->
                pathItem.readOperations().forEach(operation -> {
                    var responses = operation.getResponses();

                    // Unified Error Handling Mapping
                    responses.addApiResponse("401", createErrorResponse("Unauthorized - Invalid Credentials"));
                    responses.addApiResponse("403", createErrorResponse("Forbidden - Insufficient Scope"));
                    responses.addApiResponse("500", createErrorResponse("Internal Server Error - Trace ID Required"));
                })
        );
    }

    private ApiResponse createErrorResponse(String description) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/problem+json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))));
    }

    // =========================================================================
    // HELPERS & CONFIGURATION
    // =========================================================================

    private Info createApiInfo(OpenApiSettings s) {
        return new Info()
                .title(s.getTitle())
                .description(s.getDescription())
                .version(s.getVersion())
                .contact(new Contact().name("Core Engineering").email(s.getContactEmail()));
    }

    private List<Server> configureServers(OpenApiSettings settings) {
        if (settings.getServers().isEmpty()) {
            return List.of(new Server().url("/").description("Default Gateway"));
        }
        return settings.getServers().stream()
                .map(s -> new Server().url(s.getUrl()).description(s.getDescription()))
                .toList();
    }

    private GroupedOpenApi createGroup(String name, String displayName, String... paths) {
        return GroupedOpenApi.builder()
                .group(name)
                .displayName(displayName)
                .pathsToMatch(paths)
                .addOpenApiCustomizer(globalResponseCustomizer())
                .build();
    }

    @Data
    @Component
    @ConfigurationProperties(prefix = "app.openapi")
    public static class OpenApiSettings {
        private String title = "Logistics Orchestration Engine";
        private String description = "High-throughput logistics backbone providing real-time routing and agency management.";
        private String version = "v2.5.0-beta";
        private String contactEmail = "engineering@bytesmind.tech";
        private String issuerUri = "http://localhost:8081";
        private List<ServerInfo> servers = new ArrayList<>();

        @Data
        public static class ServerInfo {
            private String url;
            private String description;
        }
    }
}