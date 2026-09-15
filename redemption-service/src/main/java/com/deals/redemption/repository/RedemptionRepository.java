package com.deals.redemption.repository;

import com.deals.redemption.entity.Redemption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RedemptionRepository extends JpaRepository<Redemption, Long> {

    List<Redemption> findByCustomerId(Long customerId);

    List<Redemption> findByMerchantId(Long merchantId);

    List<Redemption> findByCouponId(Long couponId);
}