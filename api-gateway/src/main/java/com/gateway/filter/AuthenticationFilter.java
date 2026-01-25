package com.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * AUTHENTICATION FILTER - Custom Gateway Filter
 * 
 * This filter intercepts all requests and validates JWT tokens.
 * 
 * @Component - Marks this class as a Spring component, making it available for dependency injection.
 * 
 * GATEWAY FILTER CONCEPTS:
 * 1. Pre-filter - Executes before routing to downstream service
 * 2. Post-filter - Executes after receiving response from downstream service
 * 3. Filter Chain - Multiple filters can be chained together
 * 
 * JWT (JSON Web Token) CONCEPTS:
 * 1. Header - Contains token type and algorithm
 * 2. Payload - Contains claims (user info, roles, expiration)
 * 3. Signature - Ensures token hasn't been tampered with
 */
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Value("${jwt.secret:mySecretKeyForJWTTokenGenerationAndValidationMustBeAtLeast256Bits}")
    private String jwtSecret;

    public AuthenticationFilter() {
        super(Config.class);
    }

    /**
     * APPLY METHOD - Main filter logic
     * 
     * This method is called for each request passing through the gateway.
     * It validates the JWT token and adds user information to the request headers.
     * 
     * @param config - Filter configuration
     * @return GatewayFilter - The filter instance
     */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Check if Authorization header is present
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Missing authorization header", HttpStatus.UNAUTHORIZED);
            }

            // Extract token from Authorization header
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7); // Remove "Bearer " prefix

            try {
                // Validate and parse JWT token
                SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // Add user information to request headers for downstream services
                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("X-User-Id", claims.getSubject())
                        .header("X-User-Email", claims.get("email", String.class))
                        .header("X-User-Roles", claims.get("roles", String.class))
                        .build();

                // Continue with the modified request
                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                // Token validation failed
                return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    /**
     * ERROR HANDLER
     * 
     * Returns an error response when authentication fails.
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(message.getBytes(StandardCharsets.UTF_8)))
        );
    }

    /**
     * FILTER CONFIGURATION CLASS
     * 
     * Used to configure filter behavior if needed.
     */
    public static class Config {
        // Configuration properties can be added here
    }
}
