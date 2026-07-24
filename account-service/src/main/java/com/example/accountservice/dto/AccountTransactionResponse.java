package com.example.accountservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record AccountTransactionResponse(
        Long id,
        String eventId,
        String type,
        BigDecimal amount,
        String currency,
        Instant eventTimestamp,
        String status,
        Map<String, Object> metadata
) {
}
