package com.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * SPRING BOOT SECURITY CONFIGURATION
 * 
 * This class configures security for the API Gateway using Spring Security.
 * 
 * @Configuration - Marks this class as a Spring configuration class.
 *   Spring will process this class and create beans defined in it.
 * 
 * @EnableWebFluxSecurity - Enables Spring Security for WebFlux (reactive).
 *   Since Spring Cloud Gateway is built on WebFlux, we use reactive security.
 * 
 * SECURITY CONCEPTS:
 * 1. Authentication - Verifying who the user is (JWT token validation)
 * 2. Authorization - Verifying what the user can do (role-based access)
 * 3. CORS - Cross-Origin Resource Sharing configuration
 * 4. CSRF - Cross-Site Request Forgery protection
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * SECURITY WEB FILTER CHAIN
     * 
     * This bean configures the security filter chain for the gateway.
     * It defines which endpoints are public and which require authentication.
     * 
     * @param http - ServerHttpSecurity builder for configuring security
     * @return SecurityWebFilterChain - The configured security filter chain
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // Disable CSRF for stateless API (JWT tokens handle this)
                .csrf(csrf -> csrf.disable())
                
                // Configure authorization rules
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints - no authentication required
                        .pathMatchers("/api/auth/**", "/actuator/**").permitAll()
                        
                        // All other endpoints require authentication
                        .anyExchange().authenticated()
                )
                
                // Build and return the security filter chain
                .build();
    }
}
