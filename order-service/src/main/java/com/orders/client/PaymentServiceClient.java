package com.orders.client;

import com.orders.dto.PaymentRequest;
import com.orders.dto.PaymentResponse;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * PAYMENT SERVICE CLIENT - Feign Client with Enhanced Resilience
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
 * @Retry - Automatically retries failed requests with backoff
 * @Bulkhead - Isolates payment calls in thread pool to prevent resource starvation
 * @TimeLimiter - Fails fast if payment service is slow (connection timeout, processing delay)
 * 
 * PATTERN STACKING ORDER (applied in reverse):
 * TimeLimiter -> Bulkhead -> Retry -> CircuitBreaker -> Method
 * 
 * This ensures:
 * 1. TimeLimiter prevents long waits
 * 2. Bulkhead isolates threads
 * 3. Retry handles transient failures
 * 4. CircuitBreaker prevents cascade
 */
@FeignClient(name = "payment-service", url = "http://localhost:8082")
public interface PaymentServiceClient {

    /**
     * PROCESS PAYMENT - With Full Resilience Stack
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
     * @Retry - Retry Pattern with Exponential Backoff
     *   - Automatically retries failed requests based on configuration
     *   - Waits between retries to avoid overwhelming service
     * 
     * @Bulkhead - Thread Pool Isolation
     *   - Isolates payment calls in separate thread pool
     *   - Prevents payment service from consuming all order service threads
     *   - If bulkhead is full, fails fast
     * 
     * @TimeLimiter - Timeout Management
     *   - Fails fast if payment service is slow
     *   - Prevents order service from waiting indefinitely
     */
    @PostMapping("/api/payments/process")
    @CircuitBreaker(name = "paymentService", fallbackMethod = "processPaymentFallback")
    @Retry(name = "paymentService")
    @Bulkhead(name = "paymentService", type = Bulkhead.Type.THREADPOOL)
    @TimeLimiter(name = "paymentService")
    PaymentResponse processPayment(@RequestBody PaymentRequest request);

    /**
     * FALLBACK METHOD - Process Payment
     * 
     * Called when:
     * - Circuit breaker is OPEN (payment service is failing)
     * - All retry attempts exhausted
     * - Bulkhead is full (too many concurrent payment requests)
     * - TimeLimiter timeout exceeded (payment service is slow)
     * 
     * FALLBACK STRATEGY:
     * - Returns failure response instead of throwing exception
     * - Prevents cascading failures to order service
     * - Prevents order service from hanging
     * - Provides graceful degradation
     */
    default PaymentResponse processPaymentFallback(PaymentRequest request, Exception ex) {
        return PaymentResponse.builder()
                .success(false)
                .message("Payment service is currently unavailable or timeout. Please try again later.")
                .transactionId(null)
                .build();
    }
}
