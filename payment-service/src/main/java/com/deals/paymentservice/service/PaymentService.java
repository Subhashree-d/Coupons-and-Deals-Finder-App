package com.deals.paymentservice.service;

import com.deals.paymentservice.dto.PaymentRequest;
import com.deals.paymentservice.dto.PaymentResponse;
import com.deals.paymentservice.entity.Payment;
import com.deals.paymentservice.repository.PaymentRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    // Create payment
    public PaymentResponse createPayment(PaymentRequest request) {

        Payment payment = new Payment();

        payment.setMerchantId(request.getMerchantId());
        payment.setSubscriptionId(request.getSubscriptionId());
        payment.setAmount(request.getAmount());

        if (request.getPaymentMethod() != null) {
            payment.setPaymentMethod(request.getPaymentMethod());
        } else {
            payment.setPaymentMethod("MOCK_PAYMENT");
        }

        payment.setPaymentDate(LocalDateTime.now());

        payment.setTransactionReference(
                "TXN-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase()
        );

        payment.setStatus(Payment.PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.save(payment);

        return convertToResponse(savedPayment);
    }

    // Get payment by ID
    public PaymentResponse getPayment(Long id) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found with ID: " + id
                        ));

        return convertToResponse(payment);
    }

    // Get merchant payment history
    public List<PaymentResponse> getPaymentsByMerchant(Long merchantId) {

        return paymentRepository
                .findByMerchantId(merchantId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // Get all payments
    public List<PaymentResponse> getAllPayments() {

        return paymentRepository
                .findAll()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // Verify payment by admin
    public PaymentResponse verifyPayment(Long id, Long adminId) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found with ID: " + id
                        ));

        if (payment.getStatus() == Payment.PaymentStatus.VERIFIED) {
            throw new RuntimeException("Payment is already verified");
        }

        payment.setStatus(Payment.PaymentStatus.VERIFIED);
        payment.setVerifiedBy(adminId);
        payment.setVerifiedAt(LocalDateTime.now());

        Payment updatedPayment = paymentRepository.save(payment);

        return convertToResponse(updatedPayment);
    }

    // Reject payment
    public PaymentResponse rejectPayment(Long id, Long adminId) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found with ID: " + id
                        ));

        payment.setStatus(Payment.PaymentStatus.REJECTED);
        payment.setVerifiedBy(adminId);
        payment.setVerifiedAt(LocalDateTime.now());

        Payment updatedPayment = paymentRepository.save(payment);

        return convertToResponse(updatedPayment);
    }

    // Calculate total verified revenue
    public java.math.BigDecimal getTotalRevenue() {

        return paymentRepository.calculateTotalVerifiedRevenue();
    }

    // Convert Entity → Response
    private PaymentResponse convertToResponse(Payment payment) {

        PaymentResponse response = new PaymentResponse();

        response.setPaymentId(payment.getPaymentId());
        response.setMerchantId(payment.getMerchantId());
        response.setSubscriptionId(payment.getSubscriptionId());
        response.setAmount(payment.getAmount());
        response.setPaymentDate(payment.getPaymentDate());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setTransactionReference(
                payment.getTransactionReference()
        );
        response.setStatus(payment.getStatus());
        response.setVerifiedBy(payment.getVerifiedBy());
        response.setVerifiedAt(payment.getVerifiedAt());

        return response;
    }
}