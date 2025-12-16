package com.example.aidemo1.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger Configuration.
 * 
 * <p>Configures API documentation using springdoc-openapi library.</p>
 * 
 * <h2>Access Points</h2>
 * <ul>
 *   <li>Swagger UI: <a href="http://localhost:8080/swagger-ui.html">http://localhost:8080/swagger-ui.html</a></li>
 *   <li>OpenAPI JSON: <a href="http://localhost:8080/v3/api-docs">http://localhost:8080/v3/api-docs</a></li>
 *   <li>OpenAPI YAML: <a href="http://localhost:8080/v3/api-docs.yaml">http://localhost:8080/v3/api-docs.yaml</a></li>
 * </ul>
 * 
 * <h2>Security</h2>
 * <p>Supports both HTTP Basic and Form-based authentication for protected endpoints.</p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configures the OpenAPI specification with API metadata and security schemes.
     * 
     * <p>This configuration defines:</p>
     * <ul>
     *   <li>API title, version, and description</li>
     *   <li>Contact information for API maintainers</li>
     *   <li>License information</li>
     *   <li>Security schemes (HTTP Basic Authentication)</li>
     * </ul>
     *
     * @return configured OpenAPI instance
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Currency Exchange Rate API")
                        .version("1.0.0")
                        .description("""
                                RESTful API for managing currencies and fetching real-time exchange rates.
                                
                                ## Features
                                - Multi-currency support with ISO 4217 codes
                                - Real-time exchange rate fetching from multiple providers
                                - Rate aggregation and provider fallback
                                - Caching for improved performance
                                - Role-based access control (USER, ADMIN)
                                
                                ## Authentication
                                Protected endpoints require authentication. Use HTTP Basic Auth with valid credentials.
                                - Public: GET /api/v1/currencies, GET /api/v1/currencies/exchange-rates
                                - Protected: POST endpoints (require ADMIN role)
                                
                                ## Rate Providers
                                The system aggregates rates from multiple providers:
                                - Fixer.io API
                                - ExchangeRatesAPI.io
                                - Mock Provider 1 (testing)
                                - Mock Provider 2 (testing)
                                """)
                        .contact(new Contact()
                                .name("API Support Team")
                                .email("support@example.com")
                                .url("https://github.com/ksuvorau-g/copilot-workshop01"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
                .components(new Components()
                        .addSecuritySchemes("basicAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("HTTP Basic Authentication. Use your username and password to authenticate.")));
    }
}
