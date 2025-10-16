package com.example.aidemo1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security Configuration.
 * 
 * <p>Configures authentication and authorization rules for the application.</p>
 * 
 * <h2>Authentication</h2>
 * <ul>
 *   <li>Form-based login with default Spring Security login page</li>
 *   <li>HTTP Basic authentication for API clients</li>
 *   <li>BCrypt password encoding</li>
 *   <li>Database-backed user authentication via CustomUserDetailsService</li>
 * </ul>
 * 
 * <h2>Authorization</h2>
 * <ul>
 *   <li>Method-level security enabled via @EnableMethodSecurity</li>
 *   <li>Role-based access control with @PreAuthorize annotations</li>
 *   <li>Roles: USER, PREMIUM_USER, ADMIN</li>
 * </ul>
 * 
 * <h2>Public Endpoints (No Authentication Required)</h2>
 * <ul>
 *   <li>GET /api/v1/currencies - List all currencies</li>
 *   <li>GET /api/v1/currencies/exchange-rates - Get exchange rates</li>
 *   <li>/mock/provider1/** - Mock Provider 1 endpoints</li>
 *   <li>/mock/provider2/** - Mock Provider 2 endpoints</li>
 *   <li>/swagger-ui/** - Swagger UI</li>
 *   <li>/api-docs/** - OpenAPI documentation</li>
 *   <li>/actuator/health - Health check endpoint</li>
 * </ul>
 * 
 * <h2>Protected Endpoints (Authentication Required)</h2>
 * <ul>
 *   <li>POST /api/v1/currencies - Add currency (ADMIN only)</li>
 *   <li>POST /api/v1/currencies/refresh - Refresh rates (ADMIN only)</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Configures HTTP security for the application.
     * 
     * <p>Security features:</p>
     * <ul>
     *   <li>Form-based login with Spring Security's default login page</li>
     *   <li>HTTP Basic authentication for API clients</li>
     *   <li>Public access to GET endpoints and documentation</li>
     *   <li>Protected POST endpoints require authentication and ADMIN role</li>
     *   <li>CSRF disabled for development (enable in production)</li>
     * </ul>
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
                
                // Public GET endpoints for currencies
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/currencies").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/currencies/exchange-rates").permitAll()
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            // Disable CSRF for simplicity in development
            // TODO: Enable CSRF in production with proper token handling
            .csrf(csrf -> csrf.disable())
            
            // Configure form-based login (uses default Spring Security login page)
            .formLogin(form -> form
                .permitAll()  // Allow everyone to access login page
                .defaultSuccessUrl("/api/v1/currencies", true)  // Redirect after successful login
            )
            
            // Configure logout
            .logout(logout -> logout
                .permitAll()  // Allow everyone to logout
                .logoutSuccessUrl("/login?logout")
            )
            
            // Also support HTTP Basic for API clients (curl, Postman, etc.)
            .httpBasic(basic -> basic.realmName("Aidemo1 API"));
        
        return http.build();
    }

    /**
     * Password encoder bean for encrypting passwords.
     * Uses BCrypt hashing algorithm (strength 10).
     * 
     * <p>BCrypt is a strong, adaptive hash function designed for password storage.
     * It automatically handles salting and is resistant to brute-force attacks.</p>
     *
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
