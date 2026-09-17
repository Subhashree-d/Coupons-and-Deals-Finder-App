package com.example.notificationservice.consumer;

import com.example.notificationservice.dto.CashbackCreditedEvent;
import com.example.notificationservice.dto.CouponRedeemedEvent;
import com.example.notificationservice.dto.PaymentVerifiedEvent;
import com.example.notificationservice.entity.NotificationChannel;
import com.example.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventConsumers {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumers.class);

    private final NotificationService notificationService;

    public NotificationEventConsumers(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${rabbitmq.queue.payment-notification:notification.payment.queue}")
    public void handlePaymentVerified(PaymentVerifiedEvent event) {
        log.info("Notification Service received PaymentVerifiedEvent for merchantId: {}", event.getMerchantId());
        String subject = "Subscription Payment Verified & Activated!";
        String message = String.format("Dear Merchant (ID: %d), your payment of ₹%s (Payment ID: %d) has been verified. Your subscription (ID: %d) is now ACTIVE.",
                event.getMerchantId(), event.getAmount(), event.getPaymentId(), event.getSubscriptionId());

        notificationService.sendNotification("merchant_" + event.getMerchantId() + "@dealsplatform.com", "MERCHANT", subject, message, NotificationChannel.EMAIL);
    }

    @RabbitListener(queues = "${rabbitmq.queue.redemption-notification:notification.redemption.queue}")
    public void handleCouponRedeemed(CouponRedeemedEvent event) {
        log.info("Notification Service received CouponRedeemedEvent for customerId: {}, merchantId: {}",
                event.getCustomerId(), event.getMerchantId());

        String message = String.format("Coupon ID %d redeemed for purchase of ₹%s. Discount of ₹%s applied.",
                event.getCouponId(), event.getPurchaseAmount(), event.getDiscountAmount());

        notificationService.sendNotification("customer_" + event.getCustomerId() + "@dealsplatform.com", "CUSTOMER", "Coupon Redeemed Successfully", message, NotificationChannel.SMS);
    }

    @RabbitListener(queues = "${rabbitmq.queue.cashback-notification:notification.cashback.queue}")
    public void handleCashbackCredited(CashbackCreditedEvent event) {
        log.info("Notification Service received CashbackCreditedEvent for customerId: {}", event.getCustomerId());

        String message = String.format("Congratulations! ₹%s cashback has been added to your wallet. Your new balance is ₹%s.",
                event.getAmount(), event.getNewBalance());

        notificationService.sendNotification("customer_" + event.getCustomerId() + "@dealsplatform.com", "CUSTOMER", "Cashback Credited!", message, NotificationChannel.SMS);
    }
}
