package com.deals.coupon.controller;

import com.deals.coupon.entity.Coupon;
import com.deals.coupon.service.CouponService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping
    public ResponseEntity<Coupon> createCoupon(
            @Valid @RequestBody Coupon coupon) {

        Coupon savedCoupon = couponService.createCoupon(coupon);

        return new ResponseEntity<>(savedCoupon, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Coupon>> getAllCoupons() {

        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Coupon> getCouponById(
            @PathVariable Long id) {

        return ResponseEntity.ok(couponService.getCouponById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Coupon> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody Coupon coupon) {

        return ResponseEntity.ok(
                couponService.updateCoupon(id, coupon)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCoupon(
            @PathVariable Long id) {

        couponService.deleteCoupon(id);

        return ResponseEntity.ok("Coupon deleted successfully");
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Coupon>> getCouponsByCategory(
            @PathVariable String category) {

        return ResponseEntity.ok(
                couponService.getCouponsByCategory(category)
        );
    }

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<Coupon>> getCouponsByMerchant(
            @PathVariable Long merchantId) {

        return ResponseEntity.ok(
                couponService.getCouponsByMerchant(merchantId)
        );
    }
}