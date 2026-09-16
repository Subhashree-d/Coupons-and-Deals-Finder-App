package com.example.cashbackservice.service;

import com.example.cashbackservice.dto.*;
import com.example.cashbackservice.entity.CashbackTransaction;
import com.example.cashbackservice.entity.TransactionType;
import com.example.cashbackservice.entity.Wallet;
import com.example.cashbackservice.exception.BadRequestException;
import com.example.cashbackservice.exception.BusinessException;
import com.example.cashbackservice.repository.CashbackTransactionRepository;
import com.example.cashbackservice.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CashbackService {

    private static final Logger log = LoggerFactory.getLogger(CashbackService.class);

    private final WalletRepository walletRepository;
    private final CashbackTransactionRepository transactionRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.cashback:deals.cashback.exchange}")
    private String cashbackExchange = "deals.cashback.exchange";

    @Value("${rabbitmq.routingkey.cashback-credited:cashback.credited}")
    private String cashbackCreditedRoutingKey = "cashback.credited";

    public CashbackService(WalletRepository walletRepository,
                           CashbackTransactionRepository transactionRepository,
                           RabbitTemplate rabbitTemplate) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public WalletResponse getWalletByCustomerId(Long customerId) {
        log.info("Fetching wallet for customer ID: {}", customerId);
        Wallet wallet = walletRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet(null, customerId, BigDecimal.ZERO);
                    return walletRepository.save(newWallet);
                });
        return new WalletResponse(wallet.getWalletId(), wallet.getCustomerId(), wallet.getBalance());
    }

    public List<CashbackTransactionResponse> getTransactionsByCustomerId(Long customerId) {
        log.info("Fetching transactions for customer ID: {}", customerId);
        return transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(t -> new CashbackTransactionResponse(
                        t.getTransactionId(),
                        t.getCustomerId(),
                        t.getAmount(),
                        t.getType(),
                        t.getReferenceId(),
                        t.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void processCouponRedeemedEvent(CouponRedeemedEvent event) {
        log.info("Processing cashback for CouponRedeemedEvent: Customer ID: {}, Purchase: ₹{}, Cashback %: {}%",
                event.getCustomerId(), event.getPurchaseAmount(), event.getCashbackPercentage());

        // RULE 11: Cashback calculation (e.g. 1000 * 5% = 50)
        BigDecimal cashbackPercentage = event.getCashbackPercentage() != null ? event.getCashbackPercentage() : BigDecimal.ZERO;
        BigDecimal cashbackEarned = event.getPurchaseAmount()
                .multiply(cashbackPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        if (cashbackEarned.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Zero cashback earned for redemption ID: {}", event.getRedemptionId());
            return;
        }

        // Fetch or create wallet
        Wallet wallet = walletRepository.findByCustomerId(event.getCustomerId())
                .orElseGet(() -> new Wallet(null, event.getCustomerId(), BigDecimal.ZERO));

        // RULE 12: Wallet balance increases when cashback is earned
        wallet.setBalance(wallet.getBalance().add(cashbackEarned));
        Wallet savedWallet = walletRepository.save(wallet);

        // Record CREDIT transaction
        CashbackTransaction transaction = new CashbackTransaction();
        transaction.setCustomerId(event.getCustomerId());
        transaction.setAmount(cashbackEarned);
        transaction.setType(TransactionType.CREDIT);
        transaction.setReferenceId("REDEMPTION-" + event.getRedemptionId());
        transactionRepository.save(transaction);

        log.info("Cashback ₹{} credited to customer ID: {}. New Balance: ₹{}",
                cashbackEarned, event.getCustomerId(), savedWallet.getBalance());

        // Publish CashbackCreditedEvent
        CashbackCreditedEvent creditedEvent = new CashbackCreditedEvent(
                event.getCustomerId(),
                cashbackEarned,
                savedWallet.getBalance(),
                event.getRedemptionId(),
                LocalDateTime.now()
        );

        try {
            rabbitTemplate.convertAndSend(cashbackExchange, cashbackCreditedRoutingKey, creditedEvent);
            log.info("Published CashbackCreditedEvent for customer ID: {}", event.getCustomerId());
        } catch (Exception e) {
            log.error("Failed to publish CashbackCreditedEvent: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public WalletResponse redeemCashback(RedeemCashbackRequest request) {
        log.info("Customer ID: {} requesting cashback withdrawal/redemption: ₹{}", request.getCustomerId(), request.getAmount());

        Wallet wallet = walletRepository.findByCustomerId(request.getCustomerId())
                .orElseThrow(() -> new BadRequestException("Wallet not found for customer ID: " + request.getCustomerId()));

        // RULE 14: Customer cannot redeem more cashback than available balance
        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException("Insufficient cashback balance. Available: ₹" + wallet.getBalance() +
                    ", Requested: ₹" + request.getAmount());
        }

        // RULE 13: Wallet balance decreases when cashback is redeemed
        wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
        Wallet saved = walletRepository.save(wallet);

        // Record DEBIT transaction
        CashbackTransaction transaction = new CashbackTransaction();
        transaction.setCustomerId(request.getCustomerId());
        transaction.setAmount(request.getAmount());
        transaction.setType(TransactionType.DEBIT);
        transaction.setReferenceId("CASHBACK_REDEEMED");
        transactionRepository.save(transaction);

        log.info("Cashback redeemed successfully. Remaining balance: ₹{}", saved.getBalance());

        return new WalletResponse(saved.getWalletId(), saved.getCustomerId(), saved.getBalance());
    }
}
