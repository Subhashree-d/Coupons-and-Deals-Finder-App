package com.example.subscriptionservice.client;

import com.example.subscriptionservice.dto.PaymentRequestDto;
import com.example.subscriptionservice.dto.PaymentResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service")
public interface PaymentClient {

    @PostMapping("/api/payments")
    PaymentResponseDto createPayment(@RequestBody PaymentRequestDto request);
}
