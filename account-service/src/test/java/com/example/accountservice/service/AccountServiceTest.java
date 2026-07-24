package com.example.accountservice.service;

import com.example.accountservice.dto.AccountTransactionRequest;
import com.example.accountservice.dto.AccountTransactionResponse;
import com.example.accountservice.entity.Account;
import com.example.accountservice.entity.AccountTransaction;
import com.example.accountservice.repository.AccountRepository;
import com.example.accountservice.repository.AccountTransactionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountServiceTest {

    private AccountRepository accountRepository;
    private AccountTransactionRepository accountTransactionRepository;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        accountTransactionRepository = mock(AccountTransactionRepository.class);
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        accountService = new AccountService(accountRepository, accountTransactionRepository, meterRegistry);
    }

    @Test
    void applyTransactionShouldReturnExistingTransactionForDuplicateEventId() {
        AccountTransactionRequest request = new AccountTransactionRequest("evt-1", "acct-1", "CREDIT", new BigDecimal("100"), "USD", "2026-05-15T14:02:11Z", Map.of());
        AccountTransaction existing = new AccountTransaction("evt-1", "CREDIT", new BigDecimal("100"), "USD", Instant.parse("2026-05-15T14:02:11Z"), Map.of());

        when(accountTransactionRepository.findByEventId("evt-1")).thenReturn(Optional.of(existing));

        AccountTransactionResponse response = accountService.applyTransaction(request);

        assertEquals("evt-1", response.eventId());
    }

    @Test
    void applyTransactionShouldRejectUnknownType() {
        AccountTransactionRequest request = new AccountTransactionRequest("evt-2", "acct-1", "TRANSFER", new BigDecimal("100"), "USD", "2026-05-15T14:02:11Z", Map.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> accountService.applyTransaction(request));

        assertEquals("type must be CREDIT or DEBIT", exception.getMessage());
    }

    @Test
    void getBalanceShouldUseCreditMinusDebit() {
        Account account = new Account("acct-1", "USD");
        account.setBalance(new BigDecimal("50"));

        when(accountRepository.findById("acct-1")).thenReturn(Optional.of(account));

        BigDecimal balance = accountService.getBalance("acct-1");

        assertEquals(new BigDecimal("50"), balance);
    }
}
