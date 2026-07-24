package com.example.accountservice.controller;

import com.example.accountservice.dto.AccountResponse;
import com.example.accountservice.dto.AccountTransactionRequest;
import com.example.accountservice.dto.AccountTransactionResponse;
import com.example.accountservice.service.AccountService;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/accounts")
@Validated
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/{accountId}/transactions")
    public ResponseEntity<AccountTransactionResponse> applyTransaction(
            @PathVariable String accountId,
            @Valid @RequestBody AccountTransactionRequest request) {
        AccountTransactionResponse response = accountService.applyTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Trace-Id", MDC.get("traceId"))
                .body(response);
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable String accountId) {
        BigDecimal balance = accountService.getBalance(accountId);
        return ResponseEntity.ok()
                .header("X-Trace-Id", MDC.get("traceId"))
                .body(Map.of("accountId", accountId, "balance", balance));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountId) {
        return ResponseEntity.ok()
                .header("X-Trace-Id", MDC.get("traceId"))
                .body(accountService.getAccount(accountId));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok()
                .header("X-Trace-Id", MDC.get("traceId"))
                .body(Map.of("status", "UP"));
    }
}
