package com.orders.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * PAYMENT REQUEST DTO (Data Transfer Object)
 * 
 * DTOs are used to transfer data between services.
 * They don't contain business logic, only data.
 * 
 * @NotNull - Validation annotation: field cannot be null
 * @Positive - Validation annotation: value must be positive
 * 
 * VALIDATION CONCEPTS:
 * - Bean Validation (JSR-303) - Standard Java validation API
 * - @Valid - Triggers validation when used on method parameters
 * - Validation happens automatically in Spring MVC controllers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    private String paymentMethod;
}
