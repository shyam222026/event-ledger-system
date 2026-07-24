package com.example.accountservice.service;

import com.example.accountservice.dto.AccountResponse;
import com.example.accountservice.dto.AccountTransactionRequest;
import com.example.accountservice.dto.AccountTransactionResponse;
import com.example.accountservice.entity.Account;
import com.example.accountservice.entity.AccountTransaction;
import com.example.accountservice.repository.AccountRepository;
import com.example.accountservice.repository.AccountTransactionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository accountTransactionRepository;
    private final Counter transactionCounter;

    public AccountService(AccountRepository accountRepository, AccountTransactionRepository accountTransactionRepository, MeterRegistry meterRegistry) {
        this.accountRepository = accountRepository;
        this.accountTransactionRepository = accountTransactionRepository;
        this.transactionCounter = Counter.builder("account.transactions.applied")
                .description("Number of transactions applied by the account service")
                .register(meterRegistry);
    }

    @Transactional
    public AccountTransactionResponse applyTransaction(AccountTransactionRequest request) {
        validateRequest(request);

        Optional<AccountTransaction> existing = accountTransactionRepository.findByEventId(request.eventId());
        if (existing.isPresent()) {
            return toTransactionResponse(existing.get());
        }

        this.transactionCounter.increment();

        Account account = accountRepository.findById(request.accountId())
                .orElseGet(() -> accountRepository.save(new Account(request.accountId(), request.currency())));

        AccountTransaction transaction = new AccountTransaction(
                request.eventId(),
                request.type(),
                request.amount(),
                request.currency(),
                parseTimestamp(request.eventTimestamp()),
                request.metadata() == null ? Map.of() : request.metadata()
        );

        account.addTransaction(transaction);
        recalculateBalance(account);
        accountRepository.save(account);

        return toTransactionResponse(transaction);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return toAccountResponse(account);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return account.getBalance();
    }

    private void recalculateBalance(Account account) {
        BigDecimal balance = account.getTransactions().stream()
                .sorted(Comparator.comparing(AccountTransaction::getEventTimestamp))
                .map(transaction -> "CREDIT".equalsIgnoreCase(transaction.getType())
                        ? transaction.getAmount()
                        : transaction.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        account.setBalance(balance);
    }

    private void validateRequest(AccountTransactionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.eventId() == null || request.eventId().isBlank()) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (request.accountId() == null || request.accountId().isBlank()) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (request.type() == null || request.type().isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
        if (!"CREDIT".equalsIgnoreCase(request.type()) && !"DEBIT".equalsIgnoreCase(request.type())) {
            throw new IllegalArgumentException("type must be CREDIT or DEBIT");
        }
        parseTimestamp(request.eventTimestamp());
    }

    private Instant parseTimestamp(String eventTimestamp) {
        if (eventTimestamp == null || eventTimestamp.isBlank()) {
            throw new IllegalArgumentException("eventTimestamp is required");
        }
        try {
            return OffsetDateTime.parse(eventTimestamp).toInstant();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("eventTimestamp must be a valid ISO-8601 timestamp", exception);
        }
    }

    private AccountResponse toAccountResponse(Account account) {
        List<AccountTransactionResponse> transactions = account.getTransactions().stream()
                .sorted(Comparator.comparing(AccountTransaction::getEventTimestamp).reversed())
                .map(this::toTransactionResponse)
                .toList();
        return new AccountResponse(account.getAccountId(), account.getBalance(), account.getCurrency(), account.getCreatedAt(), transactions);
    }

    private AccountTransactionResponse toTransactionResponse(AccountTransaction transaction) {
        return new AccountTransactionResponse(
                transaction.getId(),
                transaction.getEventId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getEventTimestamp(),
                transaction.getStatus(),
                transaction.getMetadata()
        );
    }
}
