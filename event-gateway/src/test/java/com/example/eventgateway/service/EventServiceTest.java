package com.example.eventgateway.service;

import com.example.eventgateway.dto.EventRequest;
import com.example.eventgateway.dto.EventResponse;
import com.example.eventgateway.entity.EventRecord;
import com.example.eventgateway.repository.EventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventServiceTest {

    private EventRepository eventRepository;
    private RestTemplate restTemplate;
    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        restTemplate = mock(RestTemplate.class);
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        eventService = new EventService(eventRepository, restTemplate, meterRegistry);
    }

    @Test
    void submitEventShouldReturnExistingEventForDuplicateEventId() {
        EventRequest request = new EventRequest("evt-1", "acct-1", "CREDIT", new BigDecimal("100"), "USD", "2026-05-15T14:02:11Z", Map.of());
        EventRecord existing = new EventRecord("evt-1", "acct-1", "CREDIT", new BigDecimal("100"), "USD", Instant.parse("2026-05-15T14:02:11Z"), Map.of());

        when(eventRepository.findByEventId("evt-1")).thenReturn(Optional.of(existing));

        EventResponse response = eventService.submitEvent(request);

        assertEquals("evt-1", response.eventId());
        verify(restTemplate, never()).postForEntity(any(), any(), any());
    }

    @Test
    void submitEventShouldRejectInvalidEventTimestamp() {
        EventRequest request = new EventRequest("evt-2", "acct-1", "CREDIT", new BigDecimal("100"), "USD", "invalid", Map.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> eventService.submitEvent(request));

        assertEquals("eventTimestamp must be a valid ISO-8601 timestamp", exception.getMessage());
    }

    @Test
    void listEventsShouldReturnChronologicalOrder() {
        EventRecord newer = new EventRecord("evt-2", "acct-1", "CREDIT", new BigDecimal("50"), "USD", Instant.parse("2026-05-15T15:00:00Z"), Map.of());
        EventRecord older = new EventRecord("evt-1", "acct-1", "DEBIT", new BigDecimal("50"), "USD", Instant.parse("2026-05-15T14:00:00Z"), Map.of());

        when(eventRepository.findByAccountIdOrderByEventTimestampAsc("acct-1")).thenReturn(List.of(older, newer));

        List<EventResponse> responses = eventService.listEventsByAccount("acct-1");

        assertEquals(2, responses.size());
        assertEquals("evt-1", responses.get(0).eventId());
    }
}
