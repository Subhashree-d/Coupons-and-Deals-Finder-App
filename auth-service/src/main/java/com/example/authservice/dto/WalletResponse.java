package com.example.authservice.dto;

import java.math.BigDecimal;

public record WalletResponse(
        Long id,
        Long customerId,
        BigDecimal balance
) {
}
