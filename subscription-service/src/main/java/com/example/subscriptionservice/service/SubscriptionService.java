package com.example.subscriptionservice.service;

import com.example.subscriptionservice.client.PaymentClient;
import com.example.subscriptionservice.dto.*;
import com.example.subscriptionservice.entity.Subscription;
import com.example.subscriptionservice.entity.SubscriptionPlan;
import com.example.subscriptionservice.entity.SubscriptionStatus;
import com.example.subscriptionservice.exception.BadRequestException;
import com.example.subscriptionservice.exception.BusinessException;
import com.example.subscriptionservice.exception.ResourceNotFoundException;
import com.example.subscriptionservice.repository.SubscriptionPlanRepository;
import com.example.subscriptionservice.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final PaymentClient paymentClient;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               SubscriptionPlanRepository planRepository,
                               PaymentClient paymentClient) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.paymentClient = paymentClient;
    }

    public List<SubscriptionPlanResponse> getAllPlans() {
        log.info("Fetching all subscription plans");
        return planRepository.findAll().stream()
                .map(p -> new SubscriptionPlanResponse(p.getPlanId(), p.getName(), p.getDurationInMonths(), p.getPrice(), p.getCouponLimit(), p.getStatus()))
                .collect(Collectors.toList());
    }

    public SubscriptionPlanResponse getPlanById(Long planId) {
        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found with ID: " + planId));
        return new SubscriptionPlanResponse(plan.getPlanId(), plan.getName(), plan.getDurationInMonths(), plan.getPrice(), plan.getCouponLimit(), plan.getStatus());
    }

    @Transactional
    public SubscriptionPlanResponse createPlan(CreateSubscriptionPlanRequest request) {
        log.info("Creating new subscription plan: {}", request.getName());

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BadRequestException("Plan name cannot be empty");
        }

        String planName = request.getName().trim();
        if (planRepository.existsByNameIgnoreCase(planName)) {
            throw new BadRequestException("Subscription plan with name '" + planName + "' already exists");
        }

        String status = (request.getStatus() != null && !request.getStatus().trim().isEmpty())
                ? request.getStatus().trim().toUpperCase()
                : "ACTIVE";

        SubscriptionPlan plan = new SubscriptionPlan(
                null,
                planName,
                request.getDurationInMonths(),
                request.getPrice(),
                request.getCouponLimit(),
                status
        );

        SubscriptionPlan saved = planRepository.save(plan);
        log.info("Subscription plan created successfully with ID: {}", saved.getPlanId());

        return new SubscriptionPlanResponse(
                saved.getPlanId(),
                saved.getName(),
                saved.getDurationInMonths(),
                saved.getPrice(),
                saved.getCouponLimit(),
                saved.getStatus()
        );
    }

    @Transactional
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        log.info("Creating subscription for merchantId: {}, planId: {}", request.getMerchantId(), request.getPlanId());

        SubscriptionPlan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with ID: " + request.getPlanId()));

        Subscription subscription = new Subscription();
        subscription.setMerchantId(request.getMerchantId());
        subscription.setPlanId(plan.getPlanId());
        subscription.setAmount(plan.getPrice());
        subscription.setStatus(SubscriptionStatus.PENDING_VERIFICATION);

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription created with ID: {} in PENDING_VERIFICATION state (awaiting payment)", saved.getSubscriptionId());

        return mapToResponse(saved, plan);
    }

    public SubscriptionResponse getSubscriptionById(Long id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with ID: " + id));
        SubscriptionPlan plan = planRepository.findById(subscription.getPlanId()).orElse(null);
        return mapToResponse(subscription, plan);
    }

    public List<SubscriptionResponse> getSubscriptionsByMerchantId(Long merchantId) {
        return subscriptionRepository.findByMerchantId(merchantId).stream()
                .map(s -> {
                    SubscriptionPlan plan = planRepository.findById(s.getPlanId()).orElse(null);
                    return mapToResponse(s, plan);
                })
                .collect(Collectors.toList());
    }

    public SubscriptionResponse getActiveSubscription(Long merchantId) {
        log.info("Checking active subscription for merchant ID: {}", merchantId);
        Subscription subscription = subscriptionRepository.findTopByMerchantIdAndStatusOrderBySubscriptionIdDesc(merchantId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription found for merchant ID: " + merchantId));

        if (subscription.getEndDate() != null && subscription.getEndDate().isBefore(LocalDate.now())) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);
            throw new BusinessException("Subscription expired. Please renew your subscription to create a coupon.");
        }

        SubscriptionPlan plan = planRepository.findById(subscription.getPlanId()).orElse(null);
        return mapToResponse(subscription, plan);
    }

    @Transactional
    public void activateSubscriptionFromPayment(PaymentVerifiedEvent event) {
        log.info("Activating subscription via RabbitMQ event: Subscription ID {}, Merchant ID {}, Payment ID {}",
                event.getSubscriptionId(), event.getMerchantId(), event.getPaymentId());

        Subscription subscription = subscriptionRepository.findById(event.getSubscriptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with ID: " + event.getSubscriptionId()));

        if (!subscription.getMerchantId().equals(event.getMerchantId())) {
            log.error("Merchant ID mismatch: subscription belongs to merchant {}, but payment event has merchant {}",
                    subscription.getMerchantId(), event.getMerchantId());
            throw new BusinessException("Payment does not belong to the subscription's merchant");
        }

        SubscriptionPlan plan = planRepository.findById(subscription.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with ID: " + subscription.getPlanId()));

        LocalDate start = LocalDate.now();
        LocalDate end = start.plusMonths(plan.getDurationInMonths());

        subscription.setStartDate(start);
        subscription.setEndDate(end);
        subscription.setPaymentId(event.getPaymentId());
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        subscriptionRepository.save(subscription);
        log.info("Subscription ID {} successfully ACTIVATED from {} to {}", subscription.getSubscriptionId(), start, end);
    }

    private SubscriptionResponse mapToResponse(Subscription subscription, SubscriptionPlan plan) {
        String planName = plan != null ? plan.getName() : "Unknown";
        Integer couponLimit = plan != null ? plan.getCouponLimit() : null;
        return new SubscriptionResponse(
                subscription.getSubscriptionId(),
                subscription.getMerchantId(),
                subscription.getPlanId(),
                planName,
                subscription.getStartDate(),
                subscription.getEndDate(),
                subscription.getAmount(),
                subscription.getPaymentId(),
                subscription.getStatus(),
                couponLimit
        );
    }

    private SubscriptionResponse mapToResponse(Subscription subscription, String planName) {
        SubscriptionPlan plan = planRepository.findById(subscription.getPlanId()).orElse(null);
        return mapToResponse(subscription, plan);
    }
}
