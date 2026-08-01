package com.orders.client;

import com.orders.dto.InventoryRequest;
import com.orders.dto.InventoryResponse;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * INVENTORY SERVICE CLIENT - Feign Client with Enhanced Resilience
 * 
 * Synchronous REST client for Inventory Service.
 * Uses Resilience4j for circuit breaker, retry, bulkhead, and time limiter patterns.
 * 
 * RESILIENCE PATTERNS APPLIED:
 * - @CircuitBreaker: Prevents cascading failures from inventory service
 * - @Retry: Handles transient network failures and timeouts
 * - @Bulkhead: Isolates inventory calls in separate thread pool
 *   Prevents inventory service from consuming all order service threads
 * - @TimeLimiter: Fails fast if inventory service is slow
 *   Prevents order service from waiting indefinitely
 */
@FeignClient(name = "inventory-service", url = "http://localhost:8083")
public interface InventoryServiceClient {

    /**
     * RESERVE INVENTORY
     * 
     * Reserves inventory items for an order.
     * Protected by multiple resilience patterns for robustness.
     * 
     * @CircuitBreaker - Opens circuit if inventory service fails repeatedly
     * @Retry - Retries failed requests with exponential backoff
     * @Bulkhead - Isolates inventory calls with thread pool isolation
     * @TimeLimiter - Fails fast if service is slow (timeout)
     */
    @PostMapping("/api/inventory/reserve")
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "reserveInventoryFallback")
    @Retry(name = "inventoryService")
    @Bulkhead(name = "inventoryService", type = Bulkhead.Type.THREADPOOL)
    @TimeLimiter(name = "inventoryService")
    InventoryResponse reserveInventory(@RequestBody InventoryRequest request);

    /**
     * RELEASE INVENTORY (Compensation)
     * 
     * Releases reserved inventory if order fails.
     * This is part of the Saga compensation pattern.
     * 
     * Uses same resilience patterns as reserve for consistency.
     */
    @PostMapping("/api/inventory/release")
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "releaseInventoryFallback")
    @Retry(name = "inventoryService")
    @Bulkhead(name = "inventoryService", type = Bulkhead.Type.THREADPOOL)
    @TimeLimiter(name = "inventoryService")
    InventoryResponse releaseInventory(@RequestBody InventoryRequest request);

    /**
     * FALLBACK METHODS
     * 
     * Provide graceful degradation when inventory service is unavailable.
     * These are called when resilience patterns are triggered.
     */
    default InventoryResponse reserveInventoryFallback(InventoryRequest request, Exception ex) {
        return InventoryResponse.builder()
                .success(false)
                .message("Inventory service is currently unavailable - resource limit or timeout")
                .build();
    }

    default InventoryResponse releaseInventoryFallback(InventoryRequest request, Exception ex) {
        return InventoryResponse.builder()
                .success(false)
                .message("Failed to release inventory - service unavailable or timeout")
                .build();
    }
}
