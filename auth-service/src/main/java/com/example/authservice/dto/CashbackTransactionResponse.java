package com.example.authservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CashbackTransactionResponse(
        Long id,
        Long customerId,
        BigDecimal amount,
        String type,
        String description,
        LocalDateTime createdAt
) {
}
