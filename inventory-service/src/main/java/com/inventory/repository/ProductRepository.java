package com.arval.inventory.repository;

import com.arval.inventory.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * PRODUCT REPOSITORY
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
