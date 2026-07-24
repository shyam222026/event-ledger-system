package com.example.eventgateway.dto;

import java.time.Instant;

public record ApiResponse<T>(
        String status,
        T data,
        String message,
        String traceId,
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>("SUCCESS", data, null, traceId, Instant.now());
    }

    public static <T> ApiResponse<T> failure(String message, String traceId) {
        return new ApiResponse<>("ERROR", null, message, traceId, Instant.now());
    }
}
