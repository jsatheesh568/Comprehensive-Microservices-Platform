package com.orders.repository;

import com.orders.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ORDER REPOSITORY
 * 
 * Spring Data JPA Repository interface for Order entity.
 * 
 * @Repository - Marks this interface as a Spring repository.
 *   Spring automatically creates a proxy implementation at runtime.
 * 
 * JpaRepository<Order, Long> - Extends JPA repository with:
 *   - Order: Entity type
 *   - Long: Primary key type
 * 
 * SPRING DATA JPA CONCEPTS:
 * 1. Method Query Derivation - Spring automatically implements methods based on naming
 *    Example: findByStatus() creates a query: SELECT * FROM orders WHERE status = ?
 * 2. @Query - Custom JPQL or native SQL queries
 * 3. Pagination - Built-in support for pagination and sorting
 * 4. Transaction Management - All repository methods are transactional
 * 
 * BENEFITS:
 * - No need to write implementation code
 * - Type-safe queries
 * - Automatic transaction management
 * - Built-in pagination and sorting
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * FIND BY STATUS
     * 
     * Spring Data JPA automatically generates the query:
     * SELECT * FROM orders WHERE status = ?
     * 
     * Method naming convention:
     * - findBy: indicates a query
     * - Status: matches the field name (case-insensitive)
     */
    List<Order> findByStatus(Order.OrderStatus status);

    /**
     * FIND BY USER ID
     * 
     * Generates: SELECT * FROM orders WHERE user_id = ?
     */
    List<Order> findByUserId(Long userId);

    /**
     * FIND BY USER ID AND STATUS
     * 
     * Generates: SELECT * FROM orders WHERE user_id = ? AND status = ?
     * 
     * Spring automatically handles multiple conditions with "And"
     */
    List<Order> findByUserIdAndStatus(Long userId, Order.OrderStatus status);
}
