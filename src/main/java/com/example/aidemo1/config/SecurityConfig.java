package com.example.aidemo1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security Configuration.
 * 
 * <p>Configures authentication and authorization rules for the application.
 * Mock provider endpoints are publicly accessible for testing and development.</p>
 * 
 * <h2>Public Endpoints (No Authentication Required)</h2>
 * <ul>
 *   <li>/mock/provider1/** - Mock Provider 1 endpoints</li>
 *   <li>/mock/provider2/** - Mock Provider 2 endpoints</li>
 *   <li>/swagger-ui/** - Swagger UI</li>
 *   <li>/api-docs/** - OpenAPI documentation</li>
 *   <li>/actuator/health - Health check endpoint</li>
 * </ul>
 * 
 * <h2>Protected Endpoints (Authentication Required)</h2>
 * <ul>
 *   <li>/api/** - All API endpoints (will be configured in Phase 6-7)</li>
 * </ul>
 * 
 * @see org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures HTTP security for the application.
     * 
     * <p>Current configuration allows public access to mock providers and documentation
     * endpoints. This is intentional for development and testing purposes.</p>
     * 
     * <p><strong>Note:</strong> In Phase 6, this will be enhanced with proper
     * authentication (form login or JWT) and role-based authorization.</p>
     * 
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // Public endpoints - no authentication required
                .requestMatchers("/mock/provider1/**").permitAll()
                .requestMatchers("/mock/provider2/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/api-docs/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                
                // All other endpoints require authentication
                // (Will be configured in Phase 6-7)
                .anyRequest().authenticated()
            )
            // Disable CSRF for simplicity in development
            // TODO: Enable CSRF in production with proper token handling
            .csrf(csrf -> csrf.disable())
            
            // Use HTTP Basic for now (will be enhanced in Phase 6)
            .httpBasic(basic -> basic.realmName("Aidemo1 API"));
        
        return http.build();
    }
}
