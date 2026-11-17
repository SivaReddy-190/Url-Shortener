package com.project.urlshortener.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Centralized OpenAPI configuration.
 *
 * SpringDoc auto-generates the full API spec from existing Spring MVC
 * annotations (@GetMapping, @PostMapping, @RequestBody, @PathVariable, etc.)
 * and Bean Validation annotations (@NotBlank, @Email, @Size, @Pattern, etc.)
 * — no @Operation or @Schema annotations needed on controllers or DTOs.
 *
 * This class provides global metadata, JWT security scheme, and error response
 * documentation derived from {@link com.project.urlshortener.exception.GlobalExceptionHandler}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("URL Shortener API")
                        .description("A Spring Boot URL Shortener service with JWT authentication, "
                                + "Redis caching, and click analytics.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Siva Reddy")
                                .url("https://github.com/SivaReddy-190/Url-Shortener")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste the JWT token from /api/v1/auth/login or /register")));
    }

    /**
     * Registers global error responses on every endpoint, matching
     * the exceptions handled by GlobalExceptionHandler.
     * This keeps controllers completely annotation-free.
     */
    @Bean
    public OpenApiCustomizer globalErrorResponseCustomizer() {
        // All error responses share the same ApiErrorResponse schema
        Map<String, ApiResponse> errorResponses = Map.of(
                "400", errorResponse("Bad Request — validation failed or illegal argument"),
                "401", errorResponse("Unauthorized — missing or invalid JWT token"),
                "403", errorResponse("Forbidden — you do not own this resource"),
                "404", errorResponse("Not Found — short link does not exist"),
                "410", errorResponse("Gone — short link has expired"),
                "500", errorResponse("Internal Server Error")
        );

        return openApi -> openApi.getPaths().values().forEach(pathItem ->
                pathItem.readOperations().forEach(operation ->
                        errorResponses.forEach((code, response) ->
                                operation.getResponses().addApiResponse(code, response)
                        )
                )
        );
    }

    private ApiResponse errorResponse(String description) {
        Schema<?> schema = new Schema<>()
                .addProperty("status", new Schema<>().type("string").example("4xx"))
                .addProperty("error", new Schema<>().type("string").example(description));

        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(schema)));
    }
}
