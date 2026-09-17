package com.example.couponservice.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateCouponRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Discount is required")
    @PositiveOrZero(message = "Discount must be zero or positive")
    private BigDecimal discount;

    @NotNull(message = "Cashback percentage is required")
    @PositiveOrZero(message = "Cashback percentage must be zero or positive")
    private BigDecimal cashbackPercentage;

    @NotNull(message = "Minimum purchase is required")
    @PositiveOrZero(message = "Minimum purchase must be zero or positive")
    private BigDecimal minimumPurchase;

    @NotNull(message = "Valid until date is required")
    private LocalDate validUntil;

    @NotNull(message = "Usage limit is required")
    @Positive(message = "Usage limit must be at least 1")
    private Integer usageLimit;

    public UpdateCouponRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public BigDecimal getCashbackPercentage() { return cashbackPercentage; }
    public void setCashbackPercentage(BigDecimal cashbackPercentage) { this.cashbackPercentage = cashbackPercentage; }

    public BigDecimal getMinimumPurchase() { return minimumPurchase; }
    public void setMinimumPurchase(BigDecimal minimumPurchase) { this.minimumPurchase = minimumPurchase; }

    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }

    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }
}
