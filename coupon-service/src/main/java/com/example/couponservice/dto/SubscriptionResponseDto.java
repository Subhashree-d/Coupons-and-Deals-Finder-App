package com.example.couponservice.dto;

import java.time.LocalDate;

public class SubscriptionResponseDto {
    private Long subscriptionId;
    private Long merchantId;
    private String planName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;

    public SubscriptionResponseDto() {}

    public SubscriptionResponseDto(Long subscriptionId, Long merchantId, String planName, LocalDate startDate, LocalDate endDate, String status) {
        this.subscriptionId = subscriptionId;
        this.merchantId = merchantId;
        this.planName = planName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }

    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
