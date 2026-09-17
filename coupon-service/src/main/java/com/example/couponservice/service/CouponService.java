package com.example.couponservice.service;

import com.example.couponservice.client.MerchantClient;
import com.example.couponservice.client.SubscriptionClient;
import com.example.couponservice.dto.*;
import com.example.couponservice.entity.Coupon;
import com.example.couponservice.entity.CouponStatus;
import com.example.couponservice.exception.BadRequestException;
import com.example.couponservice.exception.BusinessException;
import com.example.couponservice.exception.ResourceNotFoundException;
import com.example.couponservice.repository.CouponRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponService.class);

    private final CouponRepository couponRepository;
    private final MerchantClient merchantClient;
    private final SubscriptionClient subscriptionClient;

    public CouponService(CouponRepository couponRepository,
                         MerchantClient merchantClient,
                         SubscriptionClient subscriptionClient) {
        this.couponRepository = couponRepository;
        this.merchantClient = merchantClient;
        this.subscriptionClient = subscriptionClient;
    }

    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest request) {
        log.info("Creating coupon for merchant ID: {}, Code: {}", request.getMerchantId(), request.getCouponCode());

        // RULE 1: Merchant must be APPROVED
        try {
            MerchantResponseDto merchant = merchantClient.getMerchantById(request.getMerchantId());
            if (merchant == null || !"APPROVED".equalsIgnoreCase(merchant.getStatus())) {
                throw new BusinessException("Only an APPROVED merchant can create coupons. Current merchant status: " + (merchant != null ? merchant.getStatus() : "NOT_FOUND"));
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Merchant service check skipped or warning: {}", e.getMessage());
        }

        // RULE 2 & 3: Merchant must have an ACTIVE subscription, and validity cannot exceed subscription end date
        SubscriptionResponseDto activeSub = subscriptionClient.getActiveSubscription(request.getMerchantId());
        if (activeSub == null || !"ACTIVE".equalsIgnoreCase(activeSub.getStatus())) {
            throw new BusinessException("Merchant does not have an ACTIVE subscription. Please purchase/renew a subscription.");
        }

        if (activeSub.getEndDate() != null && request.getValidUntil().isAfter(activeSub.getEndDate())) {
            throw new BusinessException("Coupon validity cannot exceed the subscription expiry date.");
        }

        if (couponRepository.findByCouponCode(request.getCouponCode()).isPresent()) {
            throw new BadRequestException("Coupon code already exists: " + request.getCouponCode());
        }

        Coupon coupon = new Coupon();
        coupon.setMerchantId(request.getMerchantId());
        coupon.setTitle(request.getTitle());
        coupon.setDescription(request.getDescription());
        coupon.setCategory(request.getCategory());
        coupon.setDiscount(request.getDiscount());
        coupon.setCashbackPercentage(request.getCashbackPercentage());
        coupon.setCouponCode(request.getCouponCode().toUpperCase());
        coupon.setMinimumPurchase(request.getMinimumPurchase());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidUntil(request.getValidUntil());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setUsageCount(0);
        coupon.setStatus(CouponStatus.PENDING_APPROVAL);

        Coupon saved = couponRepository.save(coupon);
        log.info("Coupon created successfully with ID: {} (Status: PENDING_APPROVAL)", saved.getCouponId());

        return mapToResponse(saved);
    }

    public List<CouponResponse> getActiveCoupons() {
        log.info("Fetching all active coupons for customer discovery");
        return couponRepository.findByStatus(CouponStatus.ACTIVE).stream()
                .filter(c -> !c.getValidUntil().isBefore(LocalDate.now()))
                .filter(c -> c.getUsageCount() < c.getUsageLimit())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CouponResponse> getActiveCouponsByCategory(String category) {
        return couponRepository.findByCategoryAndStatus(category, CouponStatus.ACTIVE).stream()
                .filter(c -> !c.getValidUntil().isBefore(LocalDate.now()))
                .filter(c -> c.getUsageCount() < c.getUsageLimit())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CouponResponse> getCouponsByMerchantId(Long merchantId) {
        return couponRepository.findByMerchantId(merchantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CouponResponse> getAllCouponsForAdmin() {
        return couponRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public CouponResponse getCouponById(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with ID: " + id));
        return mapToResponse(coupon);
    }

    @Transactional
    public CouponResponse updateCoupon(Long id, UpdateCouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with ID: " + id));

        coupon.setTitle(request.getTitle());
        coupon.setDescription(request.getDescription());
        coupon.setCategory(request.getCategory());
        coupon.setDiscount(request.getDiscount());
        coupon.setCashbackPercentage(request.getCashbackPercentage());
        coupon.setMinimumPurchase(request.getMinimumPurchase());
        coupon.setValidUntil(request.getValidUntil());
        coupon.setUsageLimit(request.getUsageLimit());

        Coupon saved = couponRepository.save(coupon);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with ID: " + id));
        coupon.setStatus(CouponStatus.INACTIVE);
        couponRepository.save(coupon);
    }

    @Transactional
    public CouponResponse approveCoupon(Long id, String adminEmail) {
        log.info("Admin [{}] approving coupon ID: {}", adminEmail, id);
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with ID: " + id));

        coupon.setStatus(CouponStatus.ACTIVE);
        coupon.setApprovedBy(adminEmail != null ? adminEmail : "ADMIN");

        Coupon saved = couponRepository.save(coupon);
        return mapToResponse(saved);
    }

    @Transactional
    public CouponResponse rejectCoupon(Long id, String adminEmail) {
        log.info("Admin [{}] rejecting coupon ID: {}", adminEmail, id);
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with ID: " + id));

        coupon.setStatus(CouponStatus.REJECTED);
        coupon.setApprovedBy(adminEmail != null ? adminEmail : "ADMIN");

        Coupon saved = couponRepository.save(coupon);
        return mapToResponse(saved);
    }

    @Transactional
    public void incrementUsage(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with ID: " + id));

        coupon.setUsageCount(coupon.getUsageCount() + 1);
        if (coupon.getUsageCount() >= coupon.getUsageLimit()) {
            coupon.setStatus(CouponStatus.INACTIVE);
        }
        couponRepository.save(coupon);
    }

    private CouponResponse mapToResponse(Coupon coupon) {
        CouponResponse res = new CouponResponse();
        res.setCouponId(coupon.getCouponId());
        res.setMerchantId(coupon.getMerchantId());
        res.setTitle(coupon.getTitle());
        res.setDescription(coupon.getDescription());
        res.setCategory(coupon.getCategory());
        res.setDiscount(coupon.getDiscount());
        res.setCashbackPercentage(coupon.getCashbackPercentage());
        res.setCouponCode(coupon.getCouponCode());
        res.setMinimumPurchase(coupon.getMinimumPurchase());
        res.setValidFrom(coupon.getValidFrom());
        res.setValidUntil(coupon.getValidUntil());
        res.setUsageLimit(coupon.getUsageLimit());
        res.setUsageCount(coupon.getUsageCount());
        res.setStatus(coupon.getStatus());
        res.setCreatedAt(coupon.getCreatedAt());
        res.setApprovedBy(coupon.getApprovedBy());
        return res;
    }
}
