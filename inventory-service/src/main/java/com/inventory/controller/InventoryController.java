package com.arval.inventory.controller;

import com.arval.inventory.dto.InventoryRequest;
import com.arval.inventory.dto.InventoryResponse;
import com.arval.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * INVENTORY CONTROLLER
 * 
 * REST endpoints for inventory operations.
 * Supports both reservation and release (compensation) operations.
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * RESERVE INVENTORY
     * 
     * Called by Order Service as part of Saga pattern.
     */
    @PostMapping("/reserve")
    public ResponseEntity<InventoryResponse> reserveInventory(
            @Valid @RequestBody InventoryRequest request) {
        log.info("Received inventory reservation request: {}", request);
        InventoryResponse response = inventoryService.reserveInventory(request);
        
        if (response.getSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * RELEASE INVENTORY (Compensation)
     * 
     * Called by Order Service for Saga compensation.
     */
    @PostMapping("/release")
    public ResponseEntity<InventoryResponse> releaseInventory(
            @Valid @RequestBody InventoryRequest request) {
        log.info("Received inventory release request (compensation): {}", request);
        InventoryResponse response = inventoryService.releaseInventory(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET INVENTORY
     */
    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable Long productId) {
        log.info("Fetching inventory for product: {}", productId);
        InventoryResponse response = inventoryService.getInventory(productId);
        return ResponseEntity.ok(response);
    }
}
