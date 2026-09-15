package com.deals.paymentservice.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.deals.paymentservice.dto.PaymentRequest;
import com.deals.paymentservice.dto.PaymentResponse;
import com.deals.paymentservice.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Merchant creates payment
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request) {

        PaymentResponse response = paymentService.createPayment(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Get payment by ID
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable Long id) {

        return ResponseEntity.ok(paymentService.getPayment(id));
    }

    // Get merchant payment history
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<PaymentResponse>> getMerchantPayments(
            @PathVariable Long merchantId) {

        return ResponseEntity.ok(
                paymentService.getPaymentsByMerchant(merchantId)
        );
    }

    // Admin gets all payments
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {

        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    // Admin verifies payment
    @PutMapping("/{id}/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @PathVariable Long id,
            @RequestParam Long adminId) {

        return ResponseEntity.ok(
                paymentService.verifyPayment(id, adminId)
        );
    }

    // Admin rejects payment
    @PutMapping("/{id}/reject")
    public ResponseEntity<PaymentResponse> rejectPayment(
            @PathVariable Long id,
            @RequestParam Long adminId) {

        return ResponseEntity.ok(
                paymentService.rejectPayment(id, adminId)
        );
    }

    // Revenue
    @GetMapping("/revenue")
    public ResponseEntity<BigDecimal> getRevenue() {

        return ResponseEntity.ok(paymentService.getTotalRevenue());
    }
}