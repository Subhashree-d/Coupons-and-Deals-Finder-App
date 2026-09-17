package com.example.merchantservice.client;

import com.example.merchantservice.dto.CouponResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "coupon-service")
public interface CouponClient {

    @GetMapping("/api/coupons/merchant/{merchantId}")
    List<CouponResponse> getCouponsByMerchantId(@PathVariable("merchantId") Long merchantId);
}
