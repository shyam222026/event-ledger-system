package com.example.eventgateway.service;

import com.example.eventgateway.dto.EventRequest;
import com.example.eventgateway.dto.EventResponse;
import com.example.eventgateway.entity.EventRecord;
import com.example.eventgateway.repository.EventRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.MDC;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EventService {

    private static final String ACCOUNT_SERVICE_URL = "http://localhost:8082/accounts/transactions";
    private static final String CIRCUIT_BREAKER_NAME = "accountServiceCircuitBreaker";

    private final EventRepository eventRepository;
    private final RestTemplate restTemplate;
    private final Counter eventSubmissionCounter;

    public EventService(EventRepository eventRepository, RestTemplate restTemplate, MeterRegistry meterRegistry) {
        this.eventRepository = eventRepository;
        this.restTemplate = restTemplate;
        this.eventSubmissionCounter = Counter.builder("gateway.events.submitted")
                .description("Number of events submitted through the gateway")
                .register(meterRegistry);
    }

    @Transactional
    public EventResponse submitEvent(EventRequest request) {
        validateRequest(request);

        Optional<EventRecord> existing = eventRepository.findByEventId(request.eventId());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        this.eventSubmissionCounter.increment();

        EventRecord eventRecord = new EventRecord(
                request.eventId(),
                request.accountId(),
                request.type(),
                request.amount(),
                request.currency(),
                parseTimestamp(request.eventTimestamp()),
                request.metadata() == null ? Map.of() : request.metadata()
        );
        EventRecord saved = eventRepository.saveAndFlush(eventRecord);

        try {
            applyInAccountService(saved);
            saved.markApplied();
            eventRepository.save(saved);
            return toResponse(saved);
        } catch (Exception exception) {
            saved.markFailed(exception.getMessage());
            eventRepository.save(saved);
            throw new IllegalStateException("Account service unavailable", exception);
        }
    }

    @Transactional(readOnly = true)
    public Optional<EventResponse> getEventById(Long id) {
        return eventRepository.findById(id).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> listEventsByAccount(String accountId) {
        return eventRepository.findByAccountIdOrderByEventTimestampAsc(accountId).stream()
                .map(this::toResponse)
                .toList();
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackApplyTransaction")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public void applyInAccountService(EventRecord eventRecord) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Trace-Id", MDC.get("traceId"));

        Map<String, Object> body = Map.of(
                "eventId", eventRecord.getEventId(),
                "accountId", eventRecord.getAccountId(),
                "type", eventRecord.getType(),
                "amount", eventRecord.getAmount(),
                "currency", eventRecord.getCurrency(),
                "eventTimestamp", eventRecord.getEventTimestamp().toString(),
                "metadata", eventRecord.getMetadata()
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(ACCOUNT_SERVICE_URL, request, Map.class);
    }

    public void fallbackApplyTransaction(EventRecord eventRecord, Throwable throwable) {
        throw new IllegalStateException("Account service unavailable", throwable);
    }

    private void validateRequest(EventRequest request) {
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
        if (request.currency() == null || request.currency().isBlank()) {
            throw new IllegalArgumentException("currency is required");
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

    private EventResponse toResponse(EventRecord eventRecord) {
        return new EventResponse(
                eventRecord.getId(),
                eventRecord.getEventId(),
                eventRecord.getAccountId(),
                eventRecord.getType(),
                eventRecord.getAmount(),
                eventRecord.getCurrency(),
                eventRecord.getEventTimestamp(),
                eventRecord.getStatus(),
                eventRecord.getMetadata()
        );
    }
}
