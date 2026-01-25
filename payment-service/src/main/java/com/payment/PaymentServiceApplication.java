package com.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * PAYMENT SERVICE APPLICATION
 * 
 * This service demonstrates all Resilience4j patterns:
 * - Circuit Breaker
 * - Retry
 * - Bulkhead
 * - Rate Limiter
 * - Time Limiter
 */
@SpringBootApplication
@EnableDiscoveryClient
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
