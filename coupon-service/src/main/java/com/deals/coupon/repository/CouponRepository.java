package com.deals.coupon.repository;

import com.deals.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    List<Coupon> findByCategory(String category);

    List<Coupon> findByMerchantId(Long merchantId);

    Optional<Coupon> findByCouponCode(String couponCode);
}