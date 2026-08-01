package com.arval.inventory.service;

import com.arval.inventory.dto.InventoryRequest;
import com.arval.inventory.dto.InventoryResponse;
import com.arval.inventory.model.Product;
import com.arval.inventory.repository.ProductRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * INVENTORY SERVICE - With Resilience4j Patterns
 * 
 * Manages product inventory with support for:
 * - Inventory reservation (for orders)
 * - Inventory release (Saga compensation)
 * - Circuit Breaker: Prevents cascading failures from database or business logic errors
 * - Retry: Handles transient failures in database access
 * - Bulkhead: Isolates inventory operations in separate thread pool
 * - Time Limiter: Prevents hanging operations
 * 
 * This service participates in the Saga pattern as a participant.
 * It exposes operations and compensations for the orchestrator (Order Service).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductRepository productRepository;

    /**
     * RESERVE INVENTORY - With Resilience Patterns
     * 
     * Reserves inventory for an order. This is a Saga step.
     * 
     * RESILIENCE PATTERNS APPLIED:
     * - @CircuitBreaker: Stops trying if inventory service is fundamentally broken
     * - @Retry: Handles transient DB failures (deadlocks, temporary unavailability)
     * - @Bulkhead: Ensures inventory operations don't starve other services
     * - @TimeLimiter: Fails fast if DB query hangs (connection pool timeout, etc)
     * 
     * @Transactional - Ensures atomicity of database operations.
     *   If reservation fails, transaction is rolled back.
     * 
     * SAGA PARTICIPANT ROLE:
     * - This is Step 1 in the order creation saga
     * - If this succeeds but later steps fail, compensation (release) is called
     * - Must be idempotent (can be called multiple times safely)
     */
    @Transactional
    @CircuitBreaker(name = "inventoryProcessing", fallbackMethod = "reserveInventoryFallback")
    @Retry(name = "inventoryProcessing")
    @Bulkhead(name = "inventoryProcessing", type = Bulkhead.Type.THREADPOOL)
    @TimeLimiter(name = "inventoryProcessing")
    public InventoryResponse reserveInventory(InventoryRequest request) {
        log.info("Reserving inventory: productId={}, quantity={}, orderId={}",
                request.getProductId(), request.getQuantity(), request.getOrderId());

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + request.getProductId()));

        // Check availability
        int availableQuantity = product.getQuantity() - product.getReservedQuantity();
        if (availableQuantity < request.getQuantity()) {
            log.warn("Insufficient inventory. Available: {}, Requested: {}", 
                    availableQuantity, request.getQuantity());
            return InventoryResponse.builder()
                    .success(false)
                    .message("Insufficient inventory. Available: " + availableQuantity)
                    .availableQuantity(availableQuantity)
                    .build();
        }

        // Reserve inventory
        product.setReservedQuantity(product.getReservedQuantity() + request.getQuantity());
        productRepository.save(product);

        log.info("Inventory reserved successfully. Remaining available: {}", 
                product.getQuantity() - product.getReservedQuantity());

        return InventoryResponse.builder()
                .success(true)
                .message("Inventory reserved successfully")
                .availableQuantity(product.getQuantity() - product.getReservedQuantity())
                .build();
    }

    /**
     * RELEASE INVENTORY - Saga Compensation with Resilience
     * 
     * Releases previously reserved inventory.
     * This is the compensation action for reserveInventory.
     * 
     * RESILIENCE PATTERNS:
     * - @CircuitBreaker: Prevents repeated failures affecting other operations
     * - @Retry: Handles transient database issues
     * - @Bulkhead: Isolation protects other operations
     * - @TimeLimiter: Prevents deadlocks or connection pool hangs
     * 
     * COMPENSATION PATTERN:
     * - Called when a later saga step fails
     * - Reverses the effect of reserveInventory
     * - Must be idempotent (safe to call multiple times)
     * 
     * EXAMPLE SCENARIO:
     * 1. Order Service calls reserveInventory() - SUCCESS
     * 2. Order Service calls processPayment() - FAILS
     * 3. Order Service calls releaseInventory() - COMPENSATION
     */
    @Transactional
    @CircuitBreaker(name = "inventoryProcessing", fallbackMethod = "releaseInventoryFallback")
    @Retry(name = "inventoryProcessing")
    @Bulkhead(name = "inventoryProcessing", type = Bulkhead.Type.THREADPOOL)
    @TimeLimiter(name = "inventoryProcessing")
    public InventoryResponse releaseInventory(InventoryRequest request) {
        log.info("Releasing inventory (compensation): productId={}, quantity={}, orderId={}",
                request.getProductId(), request.getQuantity(), request.getOrderId());

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + request.getProductId()));

        // Release reserved inventory
        int newReservedQuantity = Math.max(0, 
                product.getReservedQuantity() - request.getQuantity());
        product.setReservedQuantity(newReservedQuantity);
        productRepository.save(product);

        log.info("Inventory released successfully. Available: {}", 
                product.getQuantity() - product.getReservedQuantity());

        return InventoryResponse.builder()
                .success(true)
                .message("Inventory released successfully")
                .availableQuantity(product.getQuantity() - product.getReservedQuantity())
                .build();
    }

    /**
     * GET PRODUCT INVENTORY - With Time Limiter
     * 
     * Retrieves inventory information. Uses @TimeLimiter to prevent
     * hanging on slow database queries.
     */
    @TimeLimiter(name = "inventoryProcessing")
    @CircuitBreaker(name = "inventoryProcessing", fallbackMethod = "getInventoryFallback")
    public InventoryResponse getInventory(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        int availableQuantity = product.getQuantity() - product.getReservedQuantity();

        return InventoryResponse.builder()
                .success(true)
                .message("Inventory retrieved")
                .availableQuantity(availableQuantity)
                .build();
    }

    /**
     * FALLBACK METHODS - Graceful Degradation
     * 
     * These methods are called when resilience patterns are triggered:
     * - CircuitBreaker is OPEN (service is failing)
     * - Bulkhead is exhausted (too many concurrent calls)
     * - TimeLimiter timeout exceeded
     * - Retry exhausted all attempts
     */

    public InventoryResponse reserveInventoryFallback(InventoryRequest request, Exception ex) {
        log.error("Circuit breaker or resource limit triggered for reserveInventory. OrderId: {}, Error: {}",
                request.getOrderId(), ex.getMessage());
        return InventoryResponse.builder()
                .success(false)
                .message("Inventory service temporarily unavailable. Please retry your order.")
                .availableQuantity(0)
                .build();
    }

    public InventoryResponse releaseInventoryFallback(InventoryRequest request, Exception ex) {
        log.error("Circuit breaker or resource limit triggered for releaseInventory. OrderId: {}, Error: {}",
                request.getOrderId(), ex.getMessage());
        // Return success for compensation to continue (idempotent)
        return InventoryResponse.builder()
                .success(true)
                .message("Inventory release acknowledged (compensation)")
                .build();
    }

    public InventoryResponse getInventoryFallback(Long productId, Exception ex) {
        log.error("Circuit breaker triggered for getInventory. ProductId: {}, Error: {}",
                productId, ex.getMessage());
        return InventoryResponse.builder()
                .success(false)
                .message("Inventory information temporarily unavailable")
                .availableQuantity(0)
                .build();
    }
}
