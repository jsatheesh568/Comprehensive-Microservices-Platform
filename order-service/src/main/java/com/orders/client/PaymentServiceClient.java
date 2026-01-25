package com.orders.client;

import com.orders.dto.PaymentRequest;
import com.orders.dto.PaymentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * PAYMENT SERVICE CLIENT - Feign Client
 * 
 * This interface defines how to communicate with the Payment Service.
 * 
 * @FeignClient - Declares this as a Feign client.
 *   - name: Service name registered in Eureka
 *   - url: Optional fallback URL (if Eureka is not available)
 * 
 * FEIGN CLIENT CONCEPTS:
 * 1. Declarative HTTP Client - Define interface, Feign generates implementation
 * 2. Service Discovery - Automatically resolves service URLs via Eureka
 * 3. Load Balancing - Automatically distributes requests across service instances
 * 4. Request/Response Encoding - Automatic JSON serialization/deserialization
 * 5. Error Handling - Can define fallback methods
 * 
 * RESILIENCE4J INTEGRATION:
 * @CircuitBreaker - Prevents cascading failures by opening circuit when service is down
 * @Retry - Automatically retries failed requests
 * 
 * HOW IT WORKS:
 * 1. Feign generates HTTP client implementation at runtime
 * 2. Uses Eureka to find Payment Service instances
 * 3. Load balances requests across instances
 * 4. Resilience4j intercepts calls to add resilience patterns
 */
@FeignClient(name = "payment-service", url = "http://localhost:8082")
public interface PaymentServiceClient {

    /**
     * PROCESS PAYMENT
     * 
     * @PostMapping - Maps this method to HTTP POST request
     *   The path is appended to the base URL from @FeignClient
     * 
     * @RequestBody - Request body is automatically serialized to JSON
     * 
     * @CircuitBreaker - Circuit Breaker Pattern
     *   - name: Configuration name from application.yml
     *   - fallbackMethod: Method to call when circuit is open
     * 
     * CIRCUIT BREAKER STATES:
     * 1. CLOSED - Normal operation, requests pass through
     * 2. OPEN - Service is failing, requests fail fast without calling service
     * 3. HALF_OPEN - Testing if service recovered, allows limited requests
     * 
     * @Retry - Retry Pattern
     *   - name: Configuration name from application.yml
     *   Automatically retries failed requests based on configuration
     * 
     * RETRY STRATEGY:
     * - Exponential backoff: Wait time increases exponentially between retries
     * - Max attempts: Maximum number of retry attempts
     * - Retryable exceptions: Which exceptions trigger retry
     */
    @PostMapping("/api/payments/process")
    @CircuitBreaker(name = "paymentService", fallbackMethod = "processPaymentFallback")
    @Retry(name = "paymentService")
    PaymentResponse processPayment(@RequestBody PaymentRequest request);

    /**
     * FALLBACK METHOD
     * 
     * Called when circuit breaker is open or service is unavailable.
     * This prevents cascading failures and provides graceful degradation.
     * 
     * FALLBACK PATTERN:
     * - Returns default/error response instead of throwing exception
     * - Prevents service unavailability from affecting entire system
     * - Can return cached data or default values
     */
    default PaymentResponse processPaymentFallback(PaymentRequest request, Exception ex) {
        return PaymentResponse.builder()
                .success(false)
                .message("Payment service is currently unavailable. Please try again later.")
                .transactionId(null)
                .build();
    }
}
