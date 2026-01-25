package com.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * EUREKA SERVER APPLICATION
 * 
 * This is the Service Discovery Server that acts as a registry for all microservices.
 * 
 * @SpringBootApplication - This annotation combines three annotations:
 *   1. @Configuration - Marks this class as a configuration class
 *   2. @EnableAutoConfiguration - Enables Spring Boot auto-configuration
 *   3. @ComponentScan - Scans for components in the package and sub-packages
 * 
 * @EnableEurekaServer - This annotation enables the Eureka Server functionality.
 *   It tells Spring Cloud to configure this application as a Eureka Server.
 *   All microservices will register themselves with this server and can discover
 *   other services through it.
 * 
 * HOW IT WORKS:
 * 1. This server starts on port 8761 (default)
 * 2. Other microservices register themselves with this server
 * 3. Services can query this server to find the location of other services
 * 4. Provides load balancing by returning multiple instances of the same service
 * 5. Health checks ensure only healthy services are returned
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
