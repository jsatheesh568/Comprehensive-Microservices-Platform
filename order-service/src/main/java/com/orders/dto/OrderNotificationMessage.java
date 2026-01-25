package com.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * ORDER NOTIFICATION MESSAGE
 * 
 * DTO for asynchronous message sent to notification service via RabbitMQ.
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
