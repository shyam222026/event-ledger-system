package com.example.accountservice.controller;

import com.example.accountservice.dto.AccountResponse;
import com.example.accountservice.dto.AccountTransactionRequest;
import com.example.accountservice.dto.AccountTransactionResponse;
import com.example.accountservice.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountControllerTest {

    private AccountService accountService;
    private AccountController controller;

    @BeforeEach
    void setUp() {
        accountService = mock(AccountService.class);
        controller = new AccountController(accountService);
    }

    @Test
    void applyTransaction_shouldReturnCreatedResponse() {
        AccountTransactionRequest request = new AccountTransactionRequest("evt-1", "acc-1", "CREDIT", new BigDecimal("100"), "USD", "2026-05-15T14:02:11Z", Map.of());
        AccountTransactionResponse response = new AccountTransactionResponse(1L, "evt-1", "CREDIT", new BigDecimal("100"), "USD", Instant.parse("2026-05-15T14:02:11Z"), "APPLIED", Map.of());

        when(accountService.applyTransaction(request)).thenReturn(response);

        ResponseEntity<AccountTransactionResponse> result = controller.applyTransaction("acc-1", request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void getBalance_shouldReturnBalanceMap() {
        when(accountService.getBalance("acc-1")).thenReturn(new BigDecimal("250.00"));

        ResponseEntity<Map<String, Object>> result = controller.getBalance("acc-1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("acc-1", result.getBody().get("accountId"));
        assertEquals(new BigDecimal("250.00"), result.getBody().get("balance"));
    }

    @Test
    void getAccount_shouldReturnAccountResponse() {
        AccountResponse response = new AccountResponse("acc-1", new BigDecimal("250.00"), "USD", Instant.now(), java.util.List.of());
        when(accountService.getAccount("acc-1")).thenReturn(response);

        ResponseEntity<AccountResponse> result = controller.getAccount("acc-1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void health_shouldReturnUp() {
        ResponseEntity<Map<String, String>> result = controller.health();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("UP", result.getBody().get("status"));
    }
}
