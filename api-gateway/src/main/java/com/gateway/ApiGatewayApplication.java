package com.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API GATEWAY APPLICATION
 * 
 * This is the single entry point for all client requests to microservices.
 * 
 * @SpringBootApplication - Enables Spring Boot auto-configuration
 * 
 * @EnableDiscoveryClient - Enables service discovery using Eureka.
 *   This allows the gateway to discover services registered with Eureka
 *   and route requests to them dynamically without hardcoding URLs.
 * 
 * RESPONSIBILITIES:
 * 1. Route requests to appropriate microservices
 * 2. Implement security (authentication/authorization)
 * 3. Load balancing across service instances
 * 4. Request/Response transformation
 * 5. Rate limiting and throttling
 * 6. CORS handling
 * 7. API versioning
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
