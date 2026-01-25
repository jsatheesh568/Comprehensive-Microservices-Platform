package com.payment.service;

import com.payment.dto.PaymentRequest;
import com.payment.dto.PaymentResponse;
import com.payment.model.Payment;
import com.payment.repository.PaymentRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * PAYMENT SERVICE - Resilience4j Patterns Demonstration
 * 
 * This service demonstrates all Resilience4j resilience patterns:
 * 
 * 1. CIRCUIT BREAKER PATTERN
 *    - Prevents cascading failures
 *    - Opens circuit when service is failing
 *    - Fails fast when circuit is open
 *    - Automatically tries to recover (half-open state)
 * 
 * 2. RETRY PATTERN
 *    - Automatically retries failed operations
 *    - Configurable retry attempts and backoff
 *    - Exponential backoff support
 * 
 * 3. BULKHEAD PATTERN
 *    - Isolates resources (thread pools)
 *    - Prevents one failing operation from consuming all resources
 *    - Two types: ThreadPool (this example) and Semaphore
 * 
 * 4. RATE LIMITER PATTERN
 *    - Limits number of calls per time period
 *    - Prevents overwhelming downstream services
 *    - Protects against burst traffic
 * 
 * 5. TIME LIMITER PATTERN
 *    - Sets maximum execution time
 *    - Fails if operation takes too long
 *    - Works with CompletableFuture for async operations
 * 
 * ANNOTATION EXPLANATION:
 * 
 * @CircuitBreaker - Wraps method with circuit breaker
 *   - name: Configuration name from application.yml
 *   - fallbackMethod: Method to call when circuit is open
 * 
 * @Retry - Wraps method with retry logic
 *   - name: Configuration name
 *   - Retries on exceptions specified in config
 * 
 * @Bulkhead - Isolates method execution in thread pool
 *   - name: Configuration name
 *   - type: THREADPOOL (separate thread pool) or SEMAPHORE (semaphore-based)
 * 
 * @RateLimiter - Limits call rate
 *   - name: Configuration name
 *   - fallbackMethod: Called when rate limit exceeded
 * 
 * @TimeLimiter - Sets timeout for async operations
 *   - name: Configuration name
 *   - Works with CompletableFuture return type
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * PROCESS PAYMENT - Demonstrates Multiple Resilience Patterns
     * 
     * This method is protected by:
     * - Circuit Breaker: Opens if payment processing fails repeatedly
     * - Retry: Retries on transient failures
     * - Bulkhead: Isolates payment processing in separate thread pool
     * - Rate Limiter: Limits number of concurrent payment requests
     * 
     * PATTERN COMBINATION:
     * Patterns are applied in order: RateLimiter -> Bulkhead -> Retry -> CircuitBreaker -> Method
     * 
     * This ensures:
     * 1. Rate limiting prevents too many requests
     * 2. Bulkhead isolates execution
     * 3. Retry handles transient failures
     * 4. Circuit breaker prevents cascading failures
     */
    @Transactional
    @CircuitBreaker(name = "paymentProcessing", fallbackMethod = "processPaymentFallback")
    @Retry(name = "paymentProcessing")
    @Bulkhead(name = "paymentProcessing", type = Bulkhead.Type.THREADPOOL)
    @RateLimiter(name = "paymentProcessing", fallbackMethod = "processPaymentRateLimitFallback")
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for order: {}, amount: {}", request.getOrderId(), request.getAmount());

        // Simulate external payment gateway call (can fail)
        simulatePaymentGatewayCall();

        // Create payment record
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setUserId(request.getUserId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setStatus(Payment.PaymentStatus.COMPLETED);

        payment = paymentRepository.save(payment);
        log.info("Payment processed successfully. Transaction ID: {}", payment.getTransactionId());

        return PaymentResponse.builder()
                .success(true)
                .message("Payment processed successfully")
                .transactionId(payment.getTransactionId())
                .build();
    }

    /**
     * PROCESS PAYMENT ASYNC - Demonstrates Time Limiter
     * 
     * @TimeLimiter - Sets maximum execution time for async operations
     * Returns CompletableFuture which can timeout
     * 
     * ASYNC BENEFITS:
     * - Non-blocking execution
     * - Better resource utilization
     * - Can handle timeouts gracefully
     */
    @TimeLimiter(name = "paymentProcessing")
    @CircuitBreaker(name = "paymentProcessing", fallbackMethod = "processPaymentAsyncFallback")
    @Bulkhead(name = "paymentProcessing", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<PaymentResponse> processPaymentAsync(PaymentRequest request) {
        log.info("Processing payment asynchronously for order: {}", request.getOrderId());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate long-running payment processing
                Thread.sleep(2000);
                
                Payment payment = new Payment();
                payment.setOrderId(request.getOrderId());
                payment.setUserId(request.getUserId());
                payment.setAmount(request.getAmount());
                payment.setPaymentMethod(request.getPaymentMethod());
                payment.setTransactionId(UUID.randomUUID().toString());
                payment.setStatus(Payment.PaymentStatus.COMPLETED);
                
                payment = paymentRepository.save(payment);
                
                return PaymentResponse.builder()
                        .success(true)
                        .message("Payment processed asynchronously")
                        .transactionId(payment.getTransactionId())
                        .build();
            } catch (Exception e) {
                log.error("Error processing payment asynchronously", e);
                throw new RuntimeException("Payment processing failed", e);
            }
        });
    }

    /**
     * SIMULATE PAYMENT GATEWAY CALL
     * 
     * Simulates external payment gateway that can fail.
     * Used to demonstrate resilience patterns.
     */
    private void simulatePaymentGatewayCall() {
        // Simulate random failures (10% failure rate)
        if (Math.random() < 0.1) {
            log.warn("Simulated payment gateway failure");
            throw new RuntimeException("Payment gateway temporarily unavailable");
        }
        
        // Simulate network delay
        try {
            Thread.sleep(100 + (long)(Math.random() * 200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * CIRCUIT BREAKER FALLBACK
     * 
     * Called when circuit breaker is OPEN (service is failing).
     * Provides graceful degradation.
     */
    public PaymentResponse processPaymentFallback(PaymentRequest request, Exception ex) {
        log.error("Circuit breaker OPEN - Payment service unavailable. Order: {}", request.getOrderId());
        return PaymentResponse.builder()
                .success(false)
                .message("Payment service is currently unavailable. Please try again later.")
                .transactionId(null)
                .build();
    }

    /**
     * RATE LIMITER FALLBACK
     * 
     * Called when rate limit is exceeded.
     */
    public PaymentResponse processPaymentRateLimitFallback(PaymentRequest request, Exception ex) {
        log.warn("Rate limit exceeded for payment processing. Order: {}", request.getOrderId());
        return PaymentResponse.builder()
                .success(false)
                .message("Too many payment requests. Please try again later.")
                .transactionId(null)
                .build();
    }

    /**
     * ASYNC FALLBACK
     * 
     * Called when async payment processing fails or times out.
     */
    public CompletableFuture<PaymentResponse> processPaymentAsyncFallback(PaymentRequest request, Exception ex) {
        log.error("Async payment processing failed. Order: {}", request.getOrderId());
        return CompletableFuture.completedFuture(
                PaymentResponse.builder()
                        .success(false)
                        .message("Payment processing timed out or failed")
                        .transactionId(null)
                        .build()
        );
    }

    /**
     * GET PAYMENT BY TRANSACTION ID
     */
    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .map(payment -> PaymentResponse.builder()
                        .success(true)
                        .message("Payment found")
                        .transactionId(payment.getTransactionId())
                        .build())
                .orElse(PaymentResponse.builder()
                        .success(false)
                        .message("Payment not found")
                        .transactionId(null)
                        .build());
    }
}
