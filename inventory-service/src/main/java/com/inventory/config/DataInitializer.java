package com.arval.inventory.config;

import com.arval.inventory.model.Product;
import com.arval.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * DATA INITIALIZER
 * 
 * Initializes sample data on application startup.
 * 
 * @Component - Makes this a Spring component
 * 
 * CommandLineRunner - Interface for running code after Spring Boot starts.
 *   The run() method is executed after the application context is loaded.
 * 
 * This is useful for:
 * - Seeding database with initial data
 * - Running setup tasks
 * - Validating configuration
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        log.info("Initializing sample inventory data...");
        
        // Create sample products
        Product product1 = new Product();
        product1.setName("Laptop");
        product1.setQuantity(100);
        product1.setReservedQuantity(0);
        productRepository.save(product1);
        
        Product product2 = new Product();
        product2.setName("Smartphone");
        product2.setQuantity(200);
        product2.setReservedQuantity(0);
        productRepository.save(product2);
        
        Product product3 = new Product();
        product3.setName("Tablet");
        product3.setQuantity(50);
        product3.setReservedQuantity(0);
        productRepository.save(product3);
        
        log.info("Sample inventory data initialized successfully");
    }
}
