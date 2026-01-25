package com.arval.inventory.service;

import com.arval.inventory.dto.InventoryRequest;
import com.arval.inventory.dto.InventoryResponse;
import com.arval.inventory.model.Product;
import com.arval.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * INVENTORY SERVICE
 * 
 * Manages product inventory with support for:
 * - Inventory reservation (for orders)
 * - Inventory release (Saga compensation)
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
     * RESERVE INVENTORY
     * 
     * Reserves inventory for an order. This is a Saga step.
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
     * RELEASE INVENTORY - Saga Compensation
     * 
     * Releases previously reserved inventory.
     * This is the compensation action for reserveInventory.
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
     * GET PRODUCT INVENTORY
     */
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
}
