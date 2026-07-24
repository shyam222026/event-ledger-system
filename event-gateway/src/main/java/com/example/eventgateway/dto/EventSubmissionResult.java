package com.example.eventgateway.dto;

public record EventSubmissionResult(EventResponse response, boolean idempotent) {
}
