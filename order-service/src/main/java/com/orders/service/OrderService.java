package com.orders.service;

import com.orders.client.InventoryServiceClient;
import com.orders.client.PaymentServiceClient;
import com.arval.orders.dto.*;
import com.orders.dto.*;
import com.orders.model.Order;
import com.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * ORDER SERVICE - Saga Pattern Orchestrator
 * 
 * This service implements the Saga Pattern (Orchestration-based).
 * 
 * @Service - Marks this class as a Spring service component.
 *   Services contain business logic and coordinate between repositories and clients.
 * 
 * @RequiredArgsConstructor - Lombok annotation that generates a constructor
 *   with all final fields. Used for dependency injection.
 * 
 * @Slf4j - Lombok annotation that generates a logger field (log).
 * 
 * SAGA PATTERN CONCEPTS:
 * 
 * 1. WHAT IS SAGA?
 *    - A pattern for managing distributed transactions across multiple services
 *    - Instead of 2PC (Two-Phase Commit), uses compensating transactions
 *    - Each step has a corresponding compensation (rollback) action
 * 
 * 2. SAGA TYPES:
 *    a) Orchestration (this implementation):
 *       - Central orchestrator (this service) coordinates all steps
 *       - Each service exposes operations and compensations
 *       - Orchestrator knows the complete workflow
 * 
 *    b) Choreography:
 *       - Each service knows what to do next
 *       - Services communicate via events
 *       - No central coordinator
 * 
 * 3. SAGA STEPS (Order Creation):
 *    Step 1: Reserve Inventory (compensation: Release Inventory)
 *    Step 2: Process Payment (compensation: Refund Payment)
 *    Step 3: Create Order (compensation: Cancel Order)
 *    Step 4: Send Notification (no compensation needed - idempotent)
 * 
 * 4. COMPENSATION:
 *    - If any step fails, execute compensations in reverse order
 *    - Example: If payment fails after inventory reserved, release inventory
 * 
 * 5. BENEFITS:
 *    - No distributed locks
 *    - Better scalability
 *    - Services remain loosely coupled
 *    - Can handle long-running transactions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentServiceClient paymentServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    private final RabbitTemplate rabbitTemplate;

    /**
     * CREATE ORDER - Saga Orchestration
     * 
     * This method orchestrates the distributed transaction using Saga pattern.
     * 
     * @Transactional - Ensures database operations are atomic.
     *   If an exception occurs, all database changes are rolled back.
     *   However, this only applies to THIS service's database.
     *   External service calls (payment, inventory) are NOT transactional.
     *   That's why we need Saga pattern for compensation.
     * 
     * SAGA EXECUTION FLOW:
     * 1. Create order in PENDING status
     * 2. Reserve inventory (if fails → return error)
     * 3. Process payment (if fails → release inventory, return error)
     * 4. Update order to COMPLETED
     * 5. Send notification asynchronously (fire-and-forget)
     * 
     * ERROR HANDLING:
     * - If any step fails, execute compensations in reverse order
     * - Update order status to FAILED
     * - Return error response
     */
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        log.info("Starting order creation saga for user: {}, product: {}, quantity: {}",
                request.getUserId(), request.getProductId(), request.getQuantity());

        // STEP 1: Create order in PENDING status
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setProductId(request.getProductId());
        order.setQuantity(request.getQuantity());
        order.setTotalAmount(calculateTotal(request.getProductId(), request.getQuantity()));
        order.setStatus(Order.OrderStatus.PENDING);
        order = orderRepository.save(order);
        log.info("Order created with ID: {}", order.getId());

        try {
            // STEP 2: Reserve Inventory (Saga Step 1)
            log.info("Step 1: Reserving inventory for order: {}", order.getId());
            InventoryRequest inventoryRequest = InventoryRequest.builder()
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .orderId(order.getId())
                    .build();

            InventoryResponse inventoryResponse = inventoryServiceClient.reserveInventory(inventoryRequest);
            
            if (!inventoryResponse.getSuccess()) {
                log.error("Inventory reservation failed: {}", inventoryResponse.getMessage());
                order.setStatus(Order.OrderStatus.FAILED);
                orderRepository.save(order);
                throw new RuntimeException("Inventory reservation failed: " + inventoryResponse.getMessage());
            }
            log.info("Inventory reserved successfully");

            // STEP 3: Process Payment (Saga Step 2)
            log.info("Step 2: Processing payment for order: {}", order.getId());
            PaymentRequest paymentRequest = PaymentRequest.builder()
                    .orderId(order.getId())
                    .userId(request.getUserId())
                    .amount(order.getTotalAmount())
                    .paymentMethod("CREDIT_CARD")
                    .build();

            PaymentResponse paymentResponse = paymentServiceClient.processPayment(paymentRequest);
            
            if (!paymentResponse.getSuccess()) {
                log.error("Payment processing failed: {}", paymentResponse.getMessage());
                // COMPENSATION: Release inventory
                log.info("Executing compensation: Releasing inventory");
                inventoryServiceClient.releaseInventory(inventoryRequest);
                
                order.setStatus(Order.OrderStatus.FAILED);
                orderRepository.save(order);
                throw new RuntimeException("Payment processing failed: " + paymentResponse.getMessage());
            }
            log.info("Payment processed successfully. Transaction ID: {}", paymentResponse.getTransactionId());

            // STEP 4: Update order to COMPLETED
            order.setStatus(Order.OrderStatus.COMPLETED);
            order = orderRepository.save(order);
            log.info("Order {} completed successfully", order.getId());

            // STEP 5: Send notification asynchronously (Saga Step 3 - Async)
            // This is fire-and-forget, no compensation needed
            sendOrderNotification(order);

            return order;

        } catch (Exception e) {
            log.error("Saga execution failed for order: {}", order.getId(), e);
            order.setStatus(Order.OrderStatus.FAILED);
            orderRepository.save(order);
            throw new RuntimeException("Order creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * SEND ORDER NOTIFICATION - Asynchronous Communication
     * 
     * This method demonstrates ASYNCHRONOUS microservices communication using RabbitMQ.
     * 
     * ASYNC vs SYNC COMMUNICATION:
     * 
     * SYNC (REST/Feign):
     * - Request-response pattern
     * - Caller waits for response
     * - Tight coupling (caller must know about callee)
     * - Used for: Payment, Inventory (need immediate response)
     * 
     * ASYNC (RabbitMQ/Kafka):
     * - Event-driven pattern
     * - Fire-and-forget or publish-subscribe
     * - Loose coupling (services don't know about each other)
     * - Used for: Notifications, logging, analytics
     * 
     * RABBITMQ CONCEPTS:
     * 1. Exchange - Routes messages to queues
     * 2. Queue - Stores messages
     * 3. Binding - Connects exchange to queue
     * 4. Producer - Sends messages (this service)
     * 5. Consumer - Receives messages (notification service)
     * 
     * BENEFITS OF ASYNC:
     * - Non-blocking (doesn't wait for response)
     * - Better scalability
     * - Fault tolerance (messages persist in queue)
     * - Decoupling (services don't need to be available simultaneously)
     */
    private void sendOrderNotification(Order order) {
        try {
            log.info("Sending order notification asynchronously for order: {}", order.getId());
            
            // Create notification message
            OrderNotificationMessage message = OrderNotificationMessage.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .status(order.getStatus().toString())
                    .totalAmount(order.getTotalAmount())
                    .build();

            // Send message to RabbitMQ exchange
            // The notification-service will consume this message
            rabbitTemplate.convertAndSend("order.exchange", "order.created", message);
            log.info("Order notification sent to queue");
            
        } catch (Exception e) {
            // Log error but don't fail the order
            // This is fire-and-forget, so failures shouldn't affect order creation
            log.error("Failed to send notification for order: {}", order.getId(), e);
        }
    }

    /**
     * CALCULATE TOTAL
     * 
     * Simple calculation method. In real scenario, would fetch product price from service.
     */
    private BigDecimal calculateTotal(Long productId, Integer quantity) {
        // Simplified: In real scenario, fetch price from product service
        BigDecimal unitPrice = BigDecimal.valueOf(100); // Mock price
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * GET ORDER BY ID
     */
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    /**
     * GET ORDERS BY USER ID
     */
    public java.util.List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }
}
