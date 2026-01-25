package com.orders.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ORDER ENTITY
 * 
 * Represents an order in the system.
 * 
 * @Entity - Marks this class as a JPA entity.
 *   JPA (Java Persistence API) is the standard for ORM (Object-Relational Mapping).
 *   This annotation tells Spring Data JPA to manage this class as a database table.
 * 
 * @Table - Specifies the database table name.
 *   If not specified, Spring uses the class name as table name.
 * 
 * @Id - Marks the primary key field.
 * 
 * @GeneratedValue - Specifies how the primary key is generated.
 *   Strategy.IDENTITY means the database will auto-generate the ID (like AUTO_INCREMENT).
 * 
 * @Column - Maps the field to a database column.
 *   Can specify column name, constraints, etc.
 * 
 * @Enumerated - Maps Java enum to database column.
 *   EnumType.STRING stores enum as string (e.g., "PENDING") instead of ordinal (0, 1, 2).
 * 
 * Lombok Annotations:
 * @Data - Generates getters, setters, toString, equals, and hashCode
 * @NoArgsConstructor - Generates a no-argument constructor
 * @AllArgsConstructor - Generates a constructor with all fields
 */
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * ORDER STATUS ENUM
     * 
     * Represents the lifecycle states of an order in the Saga pattern.
     * 
     * PENDING - Order created, waiting for processing
     * PROCESSING - Order is being processed (Saga in progress)
     * COMPLETED - All saga steps completed successfully
     * FAILED - Saga failed, compensation executed
     * CANCELLED - Order was cancelled
     */
    public enum OrderStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    /**
     * PRE-PERSIST CALLBACK
     * 
     * @PrePersist - JPA lifecycle callback executed before entity is persisted.
     *   Automatically sets createdAt timestamp when order is first saved.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * PRE-UPDATE CALLBACK
     * 
     * @PreUpdate - JPA lifecycle callback executed before entity is updated.
     *   Automatically updates the updatedAt timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
