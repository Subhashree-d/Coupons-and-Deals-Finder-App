package com.example.cashbackservice.service;

import com.example.cashbackservice.dto.*;
import com.example.cashbackservice.entity.*;
import com.example.cashbackservice.exception.BadRequestException;
import com.example.cashbackservice.exception.BusinessException;
import com.example.cashbackservice.repository.CashbackTransactionRepository;
import com.example.cashbackservice.repository.PointAccountRepository;
import com.example.cashbackservice.repository.PointTransactionRepository;
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
    private final PointAccountRepository pointAccountRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.cashback:deals.cashback.exchange}")
    private String cashbackExchange = "deals.cashback.exchange";
    @Value("${rabbitmq.routingkey.cashback-credited:cashback.credited}")
    private String cashbackCreditedRoutingKey = "cashback.credited";

    public CashbackService(WalletRepository walletRepository,
                           CashbackTransactionRepository transactionRepository,
                           PointAccountRepository pointAccountRepository,
                           PointTransactionRepository pointTransactionRepository,
                           RabbitTemplate rabbitTemplate) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.pointAccountRepository = pointAccountRepository;
        this.pointTransactionRepository = pointTransactionRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public PointAccountResponse getPointAccount(Long customerId, String authUserId, String authUserRole) {
        validateCustomerAccess(customerId, authUserId, authUserRole);
        log.info("Fetching point account for customer ID: {}", customerId);
        PointAccount account = pointAccountRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    PointAccount newAcc = new PointAccount(null, customerId, 0);
                    return pointAccountRepository.save(newAcc);
                });
        return new PointAccountResponse(account.getCustomerId(), account.getPointsBalance());
    }

    public List<PointTransactionResponse> getPointTransactions(Long customerId, String authUserId, String authUserRole) {
        validateCustomerAccess(customerId, authUserId, authUserRole);
        log.info("Fetching point transactions for customer ID: {}", customerId);
        return pointTransactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(t -> new PointTransactionResponse(
                        t.getTransactionId(),
                        t.getCustomerId(),
                        t.getPoints(),
                        t.getType(),
                        t.getReferenceId(),
                        t.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
    @Transactional
    public void processCouponRedeemedEvent(CouponRedeemedEvent event) {
        log.info("Processing points reward for CouponRedeemedEvent: Customer ID: {}, Redemption ID: {}",
                event.getCustomerId(), event.getRedemptionId());

        String referenceId = "REDEMPTION-" + event.getRedemptionId();

        if (pointTransactionRepository.existsByReferenceId(referenceId)) {
            log.warn("Point reward already processed for reference: {}. Ignoring duplicate event.", referenceId);
            return;
        }
        PointAccount account = pointAccountRepository.findByCustomerId(event.getCustomerId())
                .orElseGet(() -> new PointAccount(null, event.getCustomerId(), 0));

        int pointsToAdd = 10;
        account.setPointsBalance(account.getPointsBalance() + pointsToAdd);
        PointAccount savedAccount = pointAccountRepository.save(account);

        PointTransaction pointTransaction = new PointTransaction();
        pointTransaction.setCustomerId(event.getCustomerId());
        pointTransaction.setPoints(pointsToAdd);
        pointTransaction.setType(TransactionType.CREDIT);
        pointTransaction.setReferenceId(referenceId);
        pointTransactionRepository.save(pointTransaction);

        log.info("+{} points credited to customer ID: {}. New Point Balance: {}",
                pointsToAdd, event.getCustomerId(), savedAccount.getPointsBalance());

        CashbackCreditedEvent creditedEvent = new CashbackCreditedEvent(
                event.getCustomerId(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                event.getRedemptionId(),
                LocalDateTime.now()
        );

        try {
            rabbitTemplate.convertAndSend(cashbackExchange, cashbackCreditedRoutingKey, creditedEvent);
        } catch (Exception e) {
            log.error("Failed to publish event after points credit: {}", e.getMessage(), e);
        }
    }
    @Transactional
    public RedeemPointsResponse redeemPoints(RedeemPointsRequest request, String authUserId, String authUserRole) {
        validateCustomerAccess(request.getCustomerId(), authUserId, authUserRole);
        log.info("Customer ID: {} requesting point conversion: {} points", request.getCustomerId(), request.getPoints());

        if (request.getPoints() == null || request.getPoints() < 100) {
            throw new BusinessException("Minimum points required for redemption is 100 points.");
        }

        if (request.getPoints() % 100 != 0) {
            throw new BusinessException("Points to redeem must be in multiples of 100 points (100 points = ₹10).");
        }

        PointAccount account = pointAccountRepository.findByCustomerId(request.getCustomerId())
                .orElseThrow(() -> new BusinessException("Point account not found for customer ID: " + request.getCustomerId()));

        if (account.getPointsBalance() < request.getPoints()) {
            throw new BusinessException("Insufficient point balance. Available: " + account.getPointsBalance() +
                    ", Requested: " + request.getPoints());
        }

        long hundredBatches = request.getPoints() / 100;
        BigDecimal walletCredit = BigDecimal.valueOf(hundredBatches * 10L).setScale(2, RoundingMode.HALF_UP);

        account.setPointsBalance(account.getPointsBalance() - request.getPoints());
        PointAccount updatedAccount = pointAccountRepository.save(account);

        String pointRef = "POINT-REDEMPTION-" + System.currentTimeMillis();
        PointTransaction pt = new PointTransaction();
        pt.setCustomerId(request.getCustomerId());
        pt.setPoints(request.getPoints());
        pt.setType(TransactionType.DEBIT);
        pt.setReferenceId(pointRef);
        pointTransactionRepository.save(pt);

        Wallet wallet = walletRepository.findByCustomerId(request.getCustomerId())
                .orElseGet(() -> new Wallet(null, request.getCustomerId(), BigDecimal.ZERO));

        wallet.setBalance(wallet.getBalance().add(walletCredit));
        Wallet savedWallet = walletRepository.save(wallet);

        String walletRef = "POINT-CONVERSION-" + System.currentTimeMillis();
        CashbackTransaction wt = new CashbackTransaction();
        wt.setCustomerId(request.getCustomerId());
        wt.setAmount(walletCredit);
        wt.setType(TransactionType.CREDIT);
        wt.setReferenceId(walletRef);
        transactionRepository.save(wt);

        log.info("Successfully converted {} points to ₹{} for customer ID: {}. Remaining Points: {}, New Wallet Balance: ₹{}",
                request.getPoints(), walletCredit, request.getCustomerId(), updatedAccount.getPointsBalance(), savedWallet.getBalance());

        return new RedeemPointsResponse(
                request.getCustomerId(),
                request.getPoints(),
                updatedAccount.getPointsBalance(),
                walletCredit,
                savedWallet.getBalance()
        );
    }public WalletResponse getWalletByCustomerId(Long customerId, String authUserId, String authUserRole) {
        validateCustomerAccess(customerId, authUserId, authUserRole);
        log.info("Fetching wallet for customer ID: {}", customerId);
        Wallet wallet = walletRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet(null, customerId, BigDecimal.ZERO);
                    return walletRepository.save(newWallet);
                });
        return new WalletResponse(wallet.getWalletId(), wallet.getCustomerId(), wallet.getBalance());
    }

    public WalletResponse getWalletByCustomerId(Long customerId) {
        return getWalletByCustomerId(customerId, null, null);
    }

    public List<CashbackTransactionResponse> getTransactionsByCustomerId(Long customerId, String authUserId, String authUserRole) {
        validateCustomerAccess(customerId, authUserId, authUserRole);
        log.info("Fetching wallet transactions for customer ID: {}", customerId);
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

    public List<CashbackTransactionResponse> getTransactionsByCustomerId(Long customerId) {
        return getTransactionsByCustomerId(customerId, null, null);
    }

    @Transactional
    public WalletResponse redeemCashback(RedeemCashbackRequest request, String authUserId, String authUserRole) {
        validateCustomerAccess(request.getCustomerId(), authUserId, authUserRole);
        log.info("Customer ID: {} requesting wallet money withdrawal: ₹{}", request.getCustomerId(), request.getAmount());

        Wallet wallet = walletRepository.findByCustomerId(request.getCustomerId())
                .orElseThrow(() -> new BadRequestException("Wallet not found for customer ID: " + request.getCustomerId()));

        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException("Insufficient wallet balance. Available: ₹" + wallet.getBalance() +
                    ", Requested: ₹" + request.getAmount());
        }

        wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
        Wallet saved = walletRepository.save(wallet);

        CashbackTransaction transaction = new CashbackTransaction();
        transaction.setCustomerId(request.getCustomerId());
        transaction.setAmount(request.getAmount());
        transaction.setType(TransactionType.DEBIT);
        transaction.setReferenceId("WITHDRAWAL-" + System.currentTimeMillis());
        transactionRepository.save(transaction);

        log.info("Wallet money ₹{} withdrawn for customer ID: {}. Remaining Balance: ₹{}",
                request.getAmount(), request.getCustomerId(), saved.getBalance());

        return new WalletResponse(saved.getWalletId(), saved.getCustomerId(), saved.getBalance());
    }
    public WalletResponse redeemCashback(RedeemCashbackRequest request) {
        return redeemCashback(request, null, null);
    }
    private void validateCustomerAccess(Long customerId, String authUserId, String authUserRole) {
        if (authUserId != null && !authUserId.isBlank() && "CUSTOMER".equalsIgnoreCase(authUserRole)) {
            if (!customerId.toString().equals(authUserId)) {
                throw new BadRequestException("Access denied: You cannot view or modify another customer's points or wallet.");
            }
        }
    }
}
