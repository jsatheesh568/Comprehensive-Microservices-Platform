package com.orders.controller;

import com.orders.dto.CreateOrderRequest;
import com.orders.model.Order;
import com.orders.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ORDER CONTROLLER - REST API Endpoints
 * 
 * This class handles HTTP requests for order operations.
 * 
 * @RestController - Combines @Controller and @ResponseBody.
 *   - @Controller: Marks this as a Spring MVC controller
 *   - @ResponseBody: Return values are serialized to JSON
 * 
 * @RequestMapping - Base path for all endpoints in this controller.
 *   All methods will have "/api/orders" prefix.
 * 
 * @RequiredArgsConstructor - Generates constructor for dependency injection.
 * 
 * @Slf4j - Provides logger instance.
 * 
 * SPRING MVC ANNOTATIONS:
 * 
 * @GetMapping - Maps HTTP GET requests
 * @PostMapping - Maps HTTP POST requests
 * @PutMapping - Maps HTTP PUT requests
 * @DeleteMapping - Maps HTTP DELETE requests
 * 
 * @PathVariable - Extracts variable from URL path
 *   Example: /api/orders/123 -> id = 123
 * 
 * @RequestParam - Extracts query parameter
 *   Example: /api/orders?userId=456 -> userId = 456
 * 
 * @RequestBody - Deserializes request body JSON to Java object
 * 
 * @Valid - Triggers validation on the request object
 *   Uses annotations like @NotNull, @Positive from the DTO
 * 
 * ResponseEntity<T> - Wraps response with HTTP status code and headers
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    /**
     * CREATE ORDER ENDPOINT
     * 
     * @PostMapping - Handles POST requests to /api/orders
     * 
     * @RequestBody - Request body is deserialized to CreateOrderRequest
     * @Valid - Triggers validation annotations in CreateOrderRequest
     * 
     * @return ResponseEntity<Order> - Returns order with HTTP 201 (CREATED) status
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("Received order creation request: {}", request);
        Order order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * GET ORDER BY ID
     * 
     * @GetMapping("/{id}") - Handles GET requests to /api/orders/{id}
     * @PathVariable - Extracts {id} from URL
     */
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        log.info("Fetching order with ID: {}", id);
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    /**
     * GET ORDERS BY USER ID
     * 
     * @RequestParam - Extracts query parameter ?userId=123
     */
    @GetMapping
    public ResponseEntity<List<Order>> getOrdersByUser(@RequestParam Long userId) {
        log.info("Fetching orders for user: {}", userId);
        List<Order> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    /**
     * AUTHENTICATION ENDPOINT (Mock)
     * 
     * In production, this would be a separate auth service.
     * This is a simplified version for demonstration.
     */
    @PostMapping("/auth/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        // Mock authentication - in production, validate credentials
        log.info("Login request for user: {}", request.getUsername());
        // Generate JWT token (simplified)
        String token = "mock-jwt-token-for-" + request.getUsername();
        return ResponseEntity.ok(token);
    }
}

/**
 * LOGIN REQUEST DTO
 */
class LoginRequest {
    private String username;
    private String password;

    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
