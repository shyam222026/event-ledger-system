package com.example.accountservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Map;

public record AccountTransactionRequest(
        @NotBlank(message = "eventId is required") String eventId,
        @NotBlank(message = "accountId is required") String accountId,
        @NotBlank(message = "type is required")
        @Pattern(regexp = "^(CREDIT|DEBIT)$", message = "type must be CREDIT or DEBIT") String type,
        @NotNull(message = "amount is required") @Positive(message = "amount must be positive") BigDecimal amount,
        @NotBlank(message = "currency is required") String currency,
        @NotBlank(message = "eventTimestamp is required")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,9})?(?:Z|[+-]\\d{2}:?\\d{2})$",
                message = "eventTimestamp must be a valid ISO-8601 timestamp") String eventTimestamp,
        Map<String, Object> metadata
) {
}
