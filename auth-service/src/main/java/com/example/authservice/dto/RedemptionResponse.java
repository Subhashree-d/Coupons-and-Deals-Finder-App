package com.example.authservice.dto;

import java.time.LocalDateTime;

public record RedemptionResponse(
        Long id,
        Long customerId,
        Long couponId,
        String couponCode,
        String status,
        LocalDateTime redeemedAt
) {
}
