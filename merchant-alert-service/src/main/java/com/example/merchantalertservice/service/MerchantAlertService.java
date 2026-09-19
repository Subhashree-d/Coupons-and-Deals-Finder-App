package com.example.merchantalertservice.service;

import com.example.merchantalertservice.client.MerchantClient;
import com.example.merchantalertservice.dto.*;
import com.example.merchantalertservice.entity.*;
import com.example.merchantalertservice.exception.BadRequestException;
import com.example.merchantalertservice.repository.AlertPreferenceRepository;
import com.example.merchantalertservice.repository.MerchantCouponNotificationRepository;
import com.example.merchantalertservice.repository.MerchantCustomerInterestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MerchantAlertService {

    private static final Logger log = LoggerFactory.getLogger(MerchantAlertService.class);

    private final MerchantCustomerInterestRepository interestRepository;
    private final MerchantCouponNotificationRepository notificationRepository;
    private final AlertPreferenceRepository preferenceRepository;
    private final MerchantClient merchantClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.notification:deals.notification.exchange}")
    private String notificationExchange = "deals.notification.exchange";

    @Value("${rabbitmq.routingkey.merchant-alert:merchant.coupon.alert}")
    private String merchantAlertRoutingKey = "merchant.coupon.alert";

    public MerchantAlertService(MerchantCustomerInterestRepository interestRepository,
                                MerchantCouponNotificationRepository notificationRepository,
                                AlertPreferenceRepository preferenceRepository,
                                MerchantClient merchantClient,
                                RabbitTemplate rabbitTemplate) {
        this.interestRepository = interestRepository;
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.merchantClient = merchantClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Records interest for a customer who redeemed a coupon from a merchant.
     */
    @Transactional
    public void recordCustomerRedemptionInterest(Long customerId, Long merchantId) {
        if (customerId == null || merchantId == null) return;
        if (!interestRepository.existsByCustomerIdAndMerchantId(customerId, merchantId)) {
            MerchantCustomerInterest interest = new MerchantCustomerInterest(customerId, merchantId, InterestSource.REDEMPTION);
            interestRepository.save(interest);
            log.info("Recorded interest (REDEMPTION): customerId={}, merchantId={}", customerId, merchantId);
        }
    }

    /**
     * Finds customers interested in the merchant, verifies preferences and deduplication, and sends alert events.
     */
    @Transactional
    public void processCouponCreatedAlerts(CouponCreatedEvent event) {
        log.info("Processing coupon created alert for merchant ID: {}, coupon ID: {}", event.getMerchantId(), event.getCouponId());

        if (event.getMerchantId() == null || event.getCouponId() == null) return;

        List<MerchantCustomerInterest> interests = interestRepository.findByMerchantId(event.getMerchantId());
        if (interests.isEmpty()) {
            log.info("No interested customers found for merchant ID: {}", event.getMerchantId());
            return;
        }

        String merchantName = null;
        try {
            MerchantResponseDto merchant = merchantClient.getMerchantById(event.getMerchantId());
            if (merchant != null) {
                merchantName = merchant.getBusinessName();
            }
        } catch (Exception e) {
            log.warn("Could not fetch merchant name for merchant ID {}: {}", event.getMerchantId(), e.getMessage());
        }

        for (MerchantCustomerInterest interest : interests) {
            Long customerId = interest.getCustomerId();

            // 1. Idempotency check: Don't notify the same customer twice for the same coupon
            if (notificationRepository.existsByCustomerIdAndCouponId(customerId, event.getCouponId())) {
                log.info("Customer {} already notified for coupon {}. Skipping.", customerId, event.getCouponId());
                continue;
            }

            // 2. Check alert preferences
            AlertPreference preference = preferenceRepository.findByCustomerId(customerId)
                    .orElseGet(() -> new AlertPreference(customerId, true, true, true));

            if (!preference.isAnyChannelEnabled()) {
                log.info("Customer {} has disabled all alert channels. Skipping.", customerId);
                continue;
            }

            // 3. Save notification record
            MerchantCouponNotification notification = new MerchantCouponNotification(
                    customerId,
                    event.getCouponId(),
                    event.getMerchantId(),
                    NotificationStatus.SENT
            );
            notificationRepository.save(notification);

            // 4. Publish MerchantCouponAlertEvent
            MerchantCouponAlertEvent alertEvent = new MerchantCouponAlertEvent(
                    customerId,
                    event.getMerchantId(),
                    merchantName,
                    event.getCouponId(),
                    event.getTitle(),
                    event.getCouponCode(),
                    event.getDiscount(),
                    event.getCategory(),
                    event.getValidUntil(),
                    preference.isEmailEnabled(),
                    preference.isSmsEnabled(),
                    preference.isInAppEnabled()
            );

            try {
                rabbitTemplate.convertAndSend(notificationExchange, merchantAlertRoutingKey, alertEvent);
                log.info("Published MerchantCouponAlertEvent to exchange: {} for customer: {}, coupon: {}",
                        notificationExchange, customerId, event.getCouponId());
            } catch (Exception e) {
                log.error("Failed to publish MerchantCouponAlertEvent to RabbitMQ: {}", e.getMessage(), e);
            }
        }
    }

    @Transactional
    public MerchantInterestResponse followMerchant(Long customerId, Long merchantId, String authUserId, String authUserRole) {
        validateCustomerAccess(customerId, authUserId, authUserRole);
        if (customerId == null || merchantId == null) {
            throw new BadRequestException("Customer ID and Merchant ID are required.");
        }

        MerchantCustomerInterest interest = interestRepository.findByCustomerIdAndMerchantId(customerId, merchantId)
                .orElseGet(() -> {
                    MerchantCustomerInterest newInterest = new MerchantCustomerInterest(customerId, merchantId, InterestSource.MANUAL_FOLLOW);
                    return interestRepository.save(newInterest);
                });

        String merchantName = null;
        try {
            MerchantResponseDto merchant = merchantClient.getMerchantById(merchantId);
            if (merchant != null) {
                merchantName = merchant.getBusinessName();
            }
        } catch (Exception ignored) {}

        return new MerchantInterestResponse(
                interest.getId(),
                interest.getCustomerId(),
                interest.getMerchantId(),
                merchantName,
                interest.getSource(),
                interest.getFollowedAt()
        );
    }

    @Transactional
    public void unfollowMerchant(Long customerId, Long merchantId, String authUserId, String authUserRole) {
        validateCustomerAccess(customerId, authUserId, authUserRole);
        if (customerId == null || merchantId == null) {
            throw new BadRequestException("Customer ID and Merchant ID are required.");
        }
        interestRepository.deleteByCustomerIdAndMerchantId(customerId, merchantId);
        log.info("Customer {} unfollowed merchant {}", customerId, merchantId);
    }

    public List<MerchantInterestResponse> getCustomerInterests(Long customerId, String authUserId, String authUserRole) {
        validateCustomerAccess(customerId, authUserId, authUserRole);
        List<MerchantCustomerInterest> list = interestRepository.findByCustomerId(customerId);
        return list.stream().map(interest -> {
            String merchantName = null;
            try {
                MerchantResponseDto merchant = merchantClient.getMerchantById(interest.getMerchantId());
                if (merchant != null) {
                    merchantName = merchant.getBusinessName();
                }
            } catch (Exception ignored) {}

            return new MerchantInterestResponse(
                    interest.getId(),
                    interest.getCustomerId(),
                    interest.getMerchantId(),
                    merchantName,
                    interest.getSource(),
                    interest.getFollowedAt()
            );
        }).collect(Collectors.toList());
    }

    public AlertPreferenceResponse getAlertPreference(Long customerId, String authUserId, String authUserRole) {
        validateCustomerAccess(customerId, authUserId, authUserRole);
        AlertPreference pref = preferenceRepository.findByCustomerId(customerId)
                .orElseGet(() -> new AlertPreference(customerId, true, true, true));
        return new AlertPreferenceResponse(
                pref.getCustomerId(),
                pref.isEmailEnabled(),
                pref.isSmsEnabled(),
                pref.isInAppEnabled(),
                pref.getUpdatedAt()
        );
    }

    @Transactional
    public AlertPreferenceResponse updateAlertPreference(UpdateAlertPreferenceRequest request, String authUserId, String authUserRole) {
        validateCustomerAccess(request.getCustomerId(), authUserId, authUserRole);
        AlertPreference pref = preferenceRepository.findByCustomerId(request.getCustomerId())
                .orElseGet(() -> new AlertPreference(request.getCustomerId(), true, true, true));

        if (request.getEmailEnabled() != null) {
            pref.setEmailEnabled(request.getEmailEnabled());
        }
        if (request.getSmsEnabled() != null) {
            pref.setSmsEnabled(request.getSmsEnabled());
        }
        if (request.getInAppEnabled() != null) {
            pref.setInAppEnabled(request.getInAppEnabled());
        }

        AlertPreference saved = preferenceRepository.save(pref);
        log.info("Updated alert preferences for customer ID: {}", saved.getCustomerId());

        return new AlertPreferenceResponse(
                saved.getCustomerId(),
                saved.isEmailEnabled(),
                saved.isSmsEnabled(),
                saved.isInAppEnabled(),
                saved.getUpdatedAt()
        );
    }

    private void validateCustomerAccess(Long customerId, String authUserId, String authUserRole) {
        if (customerId == null) {
            throw new BadRequestException("Customer ID cannot be null.");
        }
        if (authUserId != null && !authUserId.isBlank() && "CUSTOMER".equalsIgnoreCase(authUserRole)) {
            if (!customerId.toString().equals(authUserId)) {
                throw new BadRequestException("Access denied: You cannot view or modify another customer's alert data.");
            }
        }
    }
}
