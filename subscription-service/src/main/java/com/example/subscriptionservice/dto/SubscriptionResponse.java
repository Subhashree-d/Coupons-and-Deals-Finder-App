package com.example.subscriptionservice.dto;

import com.example.subscriptionservice.entity.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public class SubscriptionResponse {
    private Long subscriptionId;
    private Long merchantId;
    private Long planId;
    private String planName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal amount;
    private Long paymentId;
    private SubscriptionStatus status;
    private Integer couponLimit;

    public SubscriptionResponse() {}

    public SubscriptionResponse(Long subscriptionId, Long merchantId, Long planId, String planName, LocalDate startDate, LocalDate endDate, BigDecimal amount, Long paymentId, SubscriptionStatus status) {
        this(subscriptionId, merchantId, planId, planName, startDate, endDate, amount, paymentId, status, null);
    }

    public SubscriptionResponse(Long subscriptionId, Long merchantId, Long planId, String planName, LocalDate startDate, LocalDate endDate, BigDecimal amount, Long paymentId, SubscriptionStatus status, Integer couponLimit) {
        this.subscriptionId = subscriptionId;
        this.merchantId = merchantId;
        this.planId = planId;
        this.planName = planName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.amount = amount;
        this.paymentId = paymentId;
        this.status = status;
        this.couponLimit = couponLimit;
    }

    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }

    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public SubscriptionStatus getStatus() { return status; }
    public void setStatus(SubscriptionStatus status) { this.status = status; }

    public Integer getCouponLimit() { return couponLimit; }
    public void setCouponLimit(Integer couponLimit) { this.couponLimit = couponLimit; }
}
