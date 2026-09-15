package com.deals.redemption.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "redemptions")
public class Redemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long redemptionId;

    private Long couponId;

    private Long merchantId;

    private Long customerId;

    private LocalDateTime redeemedAt;

    private Double purchaseAmount;

    private Double discountAmount;

    private String status;

    public Redemption() {
    }

    public Long getRedemptionId() {
        return redemptionId;
    }

    public void setRedemptionId(Long redemptionId) {
        this.redemptionId = redemptionId;
    }

    public Long getCouponId() {
        return couponId;
    }

    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public LocalDateTime getRedeemedAt() {
        return redeemedAt;
    }

    public void setRedeemedAt(LocalDateTime redeemedAt) {
        this.redeemedAt = redeemedAt;
    }

    public Double getPurchaseAmount() {
        return purchaseAmount;
    }

    public void setPurchaseAmount(Double purchaseAmount) {
        this.purchaseAmount = purchaseAmount;
    }

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}