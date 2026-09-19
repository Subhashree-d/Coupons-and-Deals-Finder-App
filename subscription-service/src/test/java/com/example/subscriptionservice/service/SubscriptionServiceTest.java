package com.example.subscriptionservice.service;

import com.example.subscriptionservice.client.PaymentClient;
import com.example.subscriptionservice.dto.CreateSubscriptionRequest;
import com.example.subscriptionservice.dto.PaymentVerifiedEvent;
import com.example.subscriptionservice.dto.SubscriptionResponse;
import com.example.subscriptionservice.entity.Subscription;
import com.example.subscriptionservice.entity.SubscriptionPlan;
import com.example.subscriptionservice.entity.SubscriptionStatus;
import com.example.subscriptionservice.exception.ResourceNotFoundException;
import com.example.subscriptionservice.repository.SubscriptionPlanRepository;
import com.example.subscriptionservice.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionPlanRepository planRepository;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private SubscriptionPlan samplePlan;
    private Subscription sampleSubscription;

    @BeforeEach
    void setUp() {
        samplePlan = new SubscriptionPlan(1L, "Standard", 3, new BigDecimal("1299.00"), 25, "ACTIVE");
        sampleSubscription = new Subscription(10L, 101L, 1L, null, null, new BigDecimal("1299.00"), null, SubscriptionStatus.PENDING_VERIFICATION);
    }

    @Test
    void shouldCreateSubscriptionAsPendingVerification() {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest(101L, 1L);
        when(planRepository.findById(1L)).thenReturn(Optional.of(samplePlan));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(sampleSubscription);

        SubscriptionResponse response = subscriptionService.createSubscription(request);

        assertNotNull(response);
        assertEquals(SubscriptionStatus.PENDING_VERIFICATION, response.getStatus());
        assertEquals("Standard", response.getPlanName());
    }

    @Test
    void shouldActivateSubscriptionFromPaymentVerifiedEvent() {
        PaymentVerifiedEvent event = new PaymentVerifiedEvent(501L, 101L, 10L, new BigDecimal("1299.00"), LocalDateTime.now());

        when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(sampleSubscription));
        when(planRepository.findById(1L)).thenReturn(Optional.of(samplePlan));

        subscriptionService.activateSubscriptionFromPayment(event);

        assertEquals(SubscriptionStatus.ACTIVE, sampleSubscription.getStatus());
        assertEquals(501L, sampleSubscription.getPaymentId());
        assertNotNull(sampleSubscription.getStartDate());
        assertEquals(LocalDate.now().plusMonths(3), sampleSubscription.getEndDate());
        verify(subscriptionRepository, times(1)).save(sampleSubscription);
    }

    @Test
    void shouldReturnActiveSubscriptionForMerchant() {
        sampleSubscription.setStatus(SubscriptionStatus.ACTIVE);
        sampleSubscription.setStartDate(LocalDate.now());
        sampleSubscription.setEndDate(LocalDate.now().plusMonths(3));

        when(subscriptionRepository.findTopByMerchantIdAndStatusOrderBySubscriptionIdDesc(101L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(sampleSubscription));
        when(planRepository.findById(1L)).thenReturn(Optional.of(samplePlan));

        SubscriptionResponse response = subscriptionService.getActiveSubscription(101L);

        assertNotNull(response);
        assertEquals(SubscriptionStatus.ACTIVE, response.getStatus());
    }

    @Test
    void shouldCreateSubscriptionPlanSuccessfully() {
        com.example.subscriptionservice.dto.CreateSubscriptionPlanRequest request =
                new com.example.subscriptionservice.dto.CreateSubscriptionPlanRequest("Enterprise", 12, new BigDecimal("4999.00"), 150, "ACTIVE");

        SubscriptionPlan savedPlan = new SubscriptionPlan(5L, "Enterprise", 12, new BigDecimal("4999.00"), 150, "ACTIVE");

        when(planRepository.existsByNameIgnoreCase("Enterprise")).thenReturn(false);
        when(planRepository.save(any(SubscriptionPlan.class))).thenReturn(savedPlan);

        com.example.subscriptionservice.dto.SubscriptionPlanResponse response = subscriptionService.createPlan(request);

        assertNotNull(response);
        assertEquals(5L, response.getPlanId());
        assertEquals("Enterprise", response.getName());
        assertEquals(12, response.getDurationInMonths());
        assertEquals(new BigDecimal("4999.00"), response.getPrice());
        assertEquals(150, response.getCouponLimit());
        assertEquals("ACTIVE", response.getStatus());
        verify(planRepository, times(1)).save(any(SubscriptionPlan.class));
    }

    @Test
    void shouldThrowBadRequestExceptionWhenPlanNameAlreadyExists() {
        com.example.subscriptionservice.dto.CreateSubscriptionPlanRequest request =
                new com.example.subscriptionservice.dto.CreateSubscriptionPlanRequest("Standard", 3, new BigDecimal("1299.00"), 25, "ACTIVE");

        when(planRepository.existsByNameIgnoreCase("Standard")).thenReturn(true);

        assertThrows(com.example.subscriptionservice.exception.BadRequestException.class, () -> {
            subscriptionService.createPlan(request);
        });

        verify(planRepository, never()).save(any(SubscriptionPlan.class));
    }
}
