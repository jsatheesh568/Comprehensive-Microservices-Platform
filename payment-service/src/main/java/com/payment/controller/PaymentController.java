package com.payment.controller;

import com.payment.dto.PaymentRequest;
import com.payment.dto.PaymentResponse;
import com.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * PAYMENT CONTROLLER
 * 
 * REST endpoints for payment operations.
 * Demonstrates both sync and async payment processing.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * PROCESS PAYMENT (Synchronous)
     * 
     * This endpoint uses all resilience patterns:
     * - Circuit Breaker
     * - Retry
     * - Bulkhead
     * - Rate Limiter
     */
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        log.info("Received payment request: {}", request);
        PaymentResponse response = paymentService.processPayment(request);
        
        if (response.getSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    /**
     * PROCESS PAYMENT (Asynchronous)
     * 
     * Demonstrates async processing with Time Limiter.
     */
    @PostMapping("/process-async")
    public ResponseEntity<CompletableFuture<PaymentResponse>> processPaymentAsync(
            @Valid @RequestBody PaymentRequest request) {
        log.info("Received async payment request: {}", request);
        CompletableFuture<PaymentResponse> future = paymentService.processPaymentAsync(request);
        return ResponseEntity.accepted().body(future);
    }

    /**
     * GET PAYMENT BY TRANSACTION ID
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String transactionId) {
        log.info("Fetching payment with transaction ID: {}", transactionId);
        PaymentResponse response = paymentService.getPaymentByTransactionId(transactionId);
        
        if (response.getSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
