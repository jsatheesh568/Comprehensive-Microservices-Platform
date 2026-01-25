package com.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * ORDER NOTIFICATION MESSAGE
 * 
 * DTO for messages received from RabbitMQ.
 * This is the message format sent by Order Service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderNotificationMessage {
    private Long orderId;
    private Long userId;
    private String status;
    private BigDecimal totalAmount;
}
