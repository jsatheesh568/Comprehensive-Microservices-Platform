package com.notification.service;

import com.notification.dto.OrderNotificationMessage;
import com.notification.model.Notification;
import com.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * NOTIFICATION SERVICE
 * 
 * Handles notification processing.
 * 
 * ASYNC COMMUNICATION PATTERNS:
 * 
 * 1. MESSAGE QUEUE (RabbitMQ):
 *    - Order Service publishes messages to queue
 *    - Notification Service consumes messages
 *    - Decoupled: Services don't need to be available simultaneously
 *    - Reliable: Messages persist until consumed
 * 
 * 2. PUBLISH-SUBSCRIBE:
 *    - One message can be consumed by multiple services
 *    - Useful for event-driven architecture
 * 
 * 3. FIRE-AND-FORGET:
 *    - Order Service doesn't wait for response
 *    - Notification is sent asynchronously
 *    - No compensation needed (idempotent operation)
 * 
 * BENEFITS:
 * - Non-blocking: Order Service doesn't wait
 * - Scalable: Can process notifications independently
 * - Resilient: Messages persist if service is down
 * - Decoupled: Services don't know about each other
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * PROCESS ORDER NOTIFICATION
     * 
     * This method is called by RabbitMQ listener when a message is received.
     * 
     * @Transactional - Ensures notification is saved atomically.
     * 
     * ASYNC PROCESSING FLOW:
     * 1. Order Service sends message to RabbitMQ
     * 2. RabbitMQ stores message in queue
     * 3. Notification Service consumes message (this method)
     * 4. Notification is processed and saved
     * 5. Email/SMS/Push notification is sent (simulated)
     */
    @Transactional
    public void processOrderNotification(OrderNotificationMessage message) {
        log.info("Processing order notification: orderId={}, userId={}, status={}",
                message.getOrderId(), message.getUserId(), message.getStatus());

        // Create notification record
        Notification notification = new Notification();
        notification.setOrderId(message.getOrderId());
        notification.setUserId(message.getUserId());
        notification.setType(Notification.NotificationType.EMAIL);
        notification.setStatus(Notification.NotificationStatus.PENDING);
        notification.setMessage(String.format(
                "Order %d has been %s. Total amount: %s",
                message.getOrderId(),
                message.getStatus(),
                message.getTotalAmount()
        ));

        try {
            // Simulate sending notification (email/SMS/push)
            sendNotification(notification);

            // Update status to SENT
            notification.setStatus(Notification.NotificationStatus.SENT);
            notificationRepository.save(notification);

            log.info("Notification sent successfully for order: {}", message.getOrderId());

        } catch (Exception e) {
            log.error("Failed to send notification for order: {}", message.getOrderId(), e);
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notificationRepository.save(notification);
            // In production, might want to retry or send to dead letter queue
        }
    }

    /**
     * SEND NOTIFICATION
     * 
     * Simulates sending notification via email/SMS/push.
     * In production, this would integrate with actual notification services.
     */
    private void sendNotification(Notification notification) {
        // Simulate network delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate notification sending
        log.info("Sending {} notification to user {}: {}",
                notification.getType(),
                notification.getUserId(),
                notification.getMessage());

        // In production:
        // - Email: Send via SMTP or email service (SendGrid, AWS SES)
        // - SMS: Send via SMS gateway (Twilio, AWS SNS)
        // - Push: Send via push notification service (FCM, APNS)
    }

    /**
     * GET NOTIFICATIONS BY USER ID
     */
    public java.util.List<Notification> getNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserId(userId);
    }
}
