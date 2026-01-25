package com.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * ORDER SERVICE APPLICATION
 * 
 * This service manages orders and orchestrates distributed transactions using Saga pattern.
 * 
 * @SpringBootApplication - Enables Spring Boot auto-configuration
 * 
 * @EnableDiscoveryClient - Registers this service with Eureka and enables service discovery.
 *   This allows the service to discover other services (Payment, Inventory, etc.)
 * 
 * @EnableFeignClients - Enables Feign clients for synchronous REST communication.
 *   Feign is a declarative HTTP client that simplifies service-to-service calls.
 *   It integrates with Eureka for service discovery and load balancing.
 * 
 * @EnableAsync - Enables asynchronous method execution.
 *   Used for async communication with RabbitMQ and non-blocking operations.
 * 
 * KEY CONCEPTS:
 * 1. Saga Pattern - Orchestrates distributed transactions across multiple services
 * 2. Synchronous Communication - REST calls using Feign
 * 3. Asynchronous Communication - RabbitMQ messaging
 * 4. Service Discovery - Uses Eureka to find other services
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableAsync
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
