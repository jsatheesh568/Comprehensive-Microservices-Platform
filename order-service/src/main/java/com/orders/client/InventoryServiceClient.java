package com.orders.client;

import com.orders.dto.InventoryRequest;
import com.orders.dto.InventoryResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * INVENTORY SERVICE CLIENT - Feign Client
 * 
 * Synchronous REST client for Inventory Service.
 * Uses Resilience4j for circuit breaker and retry patterns.
 */
@FeignClient(name = "inventory-service", url = "http://localhost:8083")
public interface InventoryServiceClient {

    /**
     * RESERVE INVENTORY
     * 
     * Reserves inventory items for an order.
     * 
     * @CircuitBreaker - Opens circuit if inventory service fails repeatedly
     * @Retry - Retries failed requests with exponential backoff
     */
    @PostMapping("/api/inventory/reserve")
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "reserveInventoryFallback")
    @Retry(name = "inventoryService")
    InventoryResponse reserveInventory(@RequestBody InventoryRequest request);

    /**
     * RELEASE INVENTORY (Compensation)
     * 
     * Releases reserved inventory if order fails.
     * This is part of the Saga compensation pattern.
     */
    @PostMapping("/api/inventory/release")
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "releaseInventoryFallback")
    InventoryResponse releaseInventory(@RequestBody InventoryRequest request);

    /**
     * FALLBACK METHODS
     * 
     * Provide graceful degradation when inventory service is unavailable.
     */
    default InventoryResponse reserveInventoryFallback(InventoryRequest request, Exception ex) {
        return InventoryResponse.builder()
                .success(false)
                .message("Inventory service is currently unavailable")
                .build();
    }

    default InventoryResponse releaseInventoryFallback(InventoryRequest request, Exception ex) {
        return InventoryResponse.builder()
                .success(false)
                .message("Failed to release inventory - service unavailable")
                .build();
    }
}
