package com.deals.paymentservice.repository;

import com.deals.paymentservice.entity.Payment;
import com.deals.paymentservice.entity.Payment.PaymentStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByMerchantId(Long merchantId);

    List<Payment> findByStatus(PaymentStatus status);

    Optional<Payment> findByTransactionReference(String transactionReference);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'VERIFIED'")
    BigDecimal calculateTotalVerifiedRevenue();
}
