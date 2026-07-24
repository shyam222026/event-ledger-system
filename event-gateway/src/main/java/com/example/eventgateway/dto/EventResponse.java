package com.example.eventgateway.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record EventResponse(
        Long id,
        String eventId,
        String accountId,
        String type,
        BigDecimal amount,
        String currency,
        Instant eventTimestamp,
        String status,
        Map<String, Object> metadata
) {
}
