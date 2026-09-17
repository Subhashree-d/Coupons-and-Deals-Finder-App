package com.example.cashbackservice.service;

import com.example.cashbackservice.dto.CashbackCreditedEvent;
import com.example.cashbackservice.dto.CouponRedeemedEvent;
import com.example.cashbackservice.dto.RedeemCashbackRequest;
import com.example.cashbackservice.dto.WalletResponse;
import com.example.cashbackservice.entity.CashbackTransaction;
import com.example.cashbackservice.entity.Wallet;
import com.example.cashbackservice.exception.BusinessException;
import com.example.cashbackservice.repository.CashbackTransactionRepository;
import com.example.cashbackservice.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashbackServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private CashbackTransactionRepository transactionRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private CashbackService cashbackService;

    private Wallet sampleWallet;

    @BeforeEach
    void setUp() {
        sampleWallet = new Wallet(1L, 2L, new BigDecimal("100.00"));
    }

    @Test
    void shouldCalculateAndCreditCashbackCorrectly() {
        // Purchase: 1000, Cashback %: 5% => Cashback earned: 50.00
        CouponRedeemedEvent event = new CouponRedeemedEvent(
                10L, 1L, 101L, 2L, new BigDecimal("1000.00"), new BigDecimal("50.00"), new BigDecimal("5.00"), LocalDateTime.now()
        );

        when(walletRepository.findByCustomerId(2L)).thenReturn(Optional.of(sampleWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(sampleWallet);

        cashbackService.processCouponRedeemedEvent(event);

        // 100 + 50 = 150
        assertEquals(new BigDecimal("150.00"), sampleWallet.getBalance());
        verify(transactionRepository, times(1)).save(any(CashbackTransaction.class));
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(CashbackCreditedEvent.class));
    }

    @Test
    void shouldRedeemCashbackSuccessfully() {
        RedeemCashbackRequest req = new RedeemCashbackRequest(2L, new BigDecimal("40.00"));
        when(walletRepository.findByCustomerId(2L)).thenReturn(Optional.of(sampleWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(sampleWallet);

        WalletResponse response = cashbackService.redeemCashback(req);

        assertNotNull(response);
        // 100 - 40 = 60
        assertEquals(new BigDecimal("60.00"), sampleWallet.getBalance());
        verify(transactionRepository, times(1)).save(any(CashbackTransaction.class));
    }

    @Test
    void shouldThrowExceptionWhenRedeemingMoreThanBalance() {
        RedeemCashbackRequest req = new RedeemCashbackRequest(2L, new BigDecimal("200.00"));
        when(walletRepository.findByCustomerId(2L)).thenReturn(Optional.of(sampleWallet));

        assertThrows(BusinessException.class, () -> cashbackService.redeemCashback(req));
        verify(walletRepository, never()).save(any(Wallet.class));
    }
}
