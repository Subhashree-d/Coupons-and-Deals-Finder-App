package com.example.merchantservice.client;

import com.example.merchantservice.dto.SubscriptionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "subscription-service")
public interface SubscriptionClient {

    @GetMapping("/api/subscriptions/merchant/{merchantId}/active")
    SubscriptionResponse getActiveSubscription(@PathVariable("merchantId") Long merchantId);
}
