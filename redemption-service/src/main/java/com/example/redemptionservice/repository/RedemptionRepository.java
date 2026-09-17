package com.example.redemptionservice.repository;

import com.example.redemptionservice.entity.Redemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RedemptionRepository extends JpaRepository<Redemption, Long> {
    List<Redemption> findByCustomerId(Long customerId);
    List<Redemption> findByMerchantId(Long merchantId);
    List<Redemption> findByCouponId(Long couponId);
    boolean existsByCustomerIdAndCouponId(Long customerId, Long couponId);
}
