package com.example.accountservice.service;

import com.example.accountservice.dto.AccountTransactionRequest;
import com.example.accountservice.entity.Account;
import com.example.accountservice.entity.AccountTransaction;
import com.example.accountservice.repository.AccountRepository;
import com.example.accountservice.repository.AccountTransactionRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceExtendedTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    AccountTransactionRepository accountTransactionRepository;

    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    AccountService accountService;

    @BeforeEach
    void setup() {
        accountService = new AccountService(accountRepository, accountTransactionRepository, meterRegistry);
    }

    @Test
    void applyTransaction_newAccount_createsAccountAndApplies() {
        when(accountTransactionRepository.findByEventId("evt1")).thenReturn(Optional.empty());
        when(accountRepository.findById("acc1")).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        var req = new AccountTransactionRequest("evt1", "acc1", "CREDIT", new BigDecimal("100.00"), "USD", "2023-01-01T00:00:00Z", Map.of());
        var resp = accountService.applyTransaction(req);

        assertEquals("evt1", resp.eventId());
        verify(accountRepository, atLeastOnce()).save(any(Account.class));
    }

    @Test
    void applyTransaction_duplicate_returnsExisting() {
        AccountTransaction existing = new AccountTransaction("evt2", "DEBIT", new BigDecimal("10.00"), "USD", java.time.Instant.now(), Map.of());
        when(accountTransactionRepository.findByEventId("evt2")).thenReturn(Optional.of(existing));

        var req = new AccountTransactionRequest("evt2", "acc2", "DEBIT", new BigDecimal("10.00"), "USD", "2023-01-01T00:00:00Z", Map.of());
        var resp = accountService.applyTransaction(req);

        assertEquals("evt2", resp.eventId());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void applyTransaction_nullRequest_throws() {
        assertThrows(IllegalArgumentException.class, () -> accountService.applyTransaction(null));
    }

    @Test
    void getBalance_notFound_throws() {
        when(accountRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> accountService.getBalance("missing"));
    }

    @Test
    void getAccount_returnsAccountWithTransactionsSorted() {
        Account account = new Account("acc3", "USD");
        AccountTransaction older = new AccountTransaction("evtA", "CREDIT", new BigDecimal("10"), "USD", java.time.Instant.parse("2026-01-01T00:00:00Z"), Map.of());
        AccountTransaction newer = new AccountTransaction("evtB", "DEBIT", new BigDecimal("5"), "USD", java.time.Instant.parse("2026-02-01T00:00:00Z"), Map.of());
        account.addTransaction(older);
        account.addTransaction(newer);
        account.setBalance(new BigDecimal("5"));
        when(accountRepository.findById("acc3")).thenReturn(Optional.of(account));

        var response = accountService.getAccount("acc3");

        assertEquals("acc3", response.accountId());
        assertEquals(2, response.transactions().size());
        assertEquals("evtB", response.transactions().get(0).eventId());
    }
}
