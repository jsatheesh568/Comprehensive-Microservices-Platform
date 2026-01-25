package com.notification.listener;

import com.notification.dto.OrderNotificationMessage;
import com.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * ORDER NOTIFICATION LISTENER - RabbitMQ Message Consumer
 * 
 * This class demonstrates ASYNCHRONOUS microservices communication.
 * 
 * @Component - Marks this as a Spring component
 * 
 * @RabbitListener - Marks a method as a RabbitMQ message consumer.
 *   - queues: List of queue names to listen to
 *   - This method is called automatically when a message arrives
 * 
 * RABBITMQ CONCEPTS:
 * 
 * 1. EXCHANGE:
 *    - Routes messages to queues based on routing key
 *    - Types: Direct, Topic, Fanout, Headers
 *    - In this case: "order.exchange" routes to "order.created" queue
 * 
 * 2. QUEUE:
 *    - Stores messages until consumed
 *    - Durable: Survives broker restart
 *    - Exclusive: Only one consumer
 * 
 * 3. BINDING:
 *    - Connects exchange to queue
 *    - Routing key: "order.created"
 * 
 * 4. MESSAGE FLOW:
 *    Order Service -> Exchange -> Queue -> This Listener
 * 
 * ASYNC vs SYNC:
 * 
 * SYNC (REST/Feign):
 * - Order Service calls Notification Service directly
 * - Waits for response
 * - Tight coupling
 * - Both services must be available
 * 
 * ASYNC (RabbitMQ):
 * - Order Service sends message to queue
 * - Doesn't wait for response
 * - Loose coupling
 * - Services can be unavailable temporarily
 * 
 * BENEFITS OF ASYNC:
 * - Non-blocking: Order creation doesn't wait for notification
 * - Resilient: Messages persist if consumer is down
 * - Scalable: Multiple consumers can process messages
 * - Decoupled: Services don't know about each other
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationListener {

    private final NotificationService notificationService;

    /**
     * LISTEN TO ORDER CREATED MESSAGES
     * 
     * @RabbitListener - Automatically consumes messages from the queue.
     *   When Order Service sends a message to "order.exchange" with routing key
     *   "order.created", this method is invoked.
     * 
     * MESSAGE PROCESSING:
     * - Message is automatically deserialized from JSON to OrderNotificationMessage
     * - Method processes the message
     * - If method throws exception, message is rejected (can be retried or sent to DLQ)
     * 
     * ACKNOWLEDGMENT:
     * - By default, Spring AMQP uses auto-acknowledgment
     * - Message is acknowledged after method completes successfully
     * - If exception is thrown, message is rejected
     */
    @RabbitListener(queues = "order.created.queue")
    public void handleOrderCreated(OrderNotificationMessage message) {
        log.info("Received order notification message: {}", message);
        try {
            notificationService.processOrderNotification(message);
            log.info("Successfully processed order notification for order: {}", message.getOrderId());
        } catch (Exception e) {
            log.error("Error processing order notification: {}", message.getOrderId(), e);
            // In production, might want to send to dead letter queue or retry
            throw e; // Re-throw to reject message (will be retried or sent to DLQ)
        }
    }
}
