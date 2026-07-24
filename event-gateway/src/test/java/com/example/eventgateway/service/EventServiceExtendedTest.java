package com.example.eventgateway.service;

import com.example.eventgateway.dto.EventRequest;
import com.example.eventgateway.entity.EventRecord;
import com.example.eventgateway.repository.EventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceExtendedTest {

    @Mock
    EventRepository eventRepository;

    @Mock
    RestTemplate restTemplate;

    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    EventService eventService;

    @BeforeEach
    void setup() {
        eventService = new EventService(eventRepository, restTemplate, meterRegistry);
    }

    @Test
    void submitEvent_nullRequest_throws() {
        assertThrows(IllegalArgumentException.class, () -> eventService.submitEvent(null));
    }

    @Test
    void submitEvent_invalidTimestamp_throws() {
        EventRequest req = new EventRequest("e1", "a1", "CREDIT", BigDecimal.ONE, "USD", "not-a-timestamp", Map.of());
        assertThrows(IllegalArgumentException.class, () -> eventService.submitEvent(req));
    }

    @Test
    void submitEvent_existing_returnsExisting() {
        EventRecord record = new EventRecord("e1", "a1", "CREDIT", BigDecimal.ONE, "USD", java.time.Instant.now(), Map.of());
        when(eventRepository.findByEventId("e1")).thenReturn(Optional.of(record));

        var response = eventService.submitEvent(new EventRequest("e1", "a1", "CREDIT", BigDecimal.ONE, "USD", "2023-01-01T00:00:00Z", Map.of()));

        assertEquals("e1", response.eventId());
        verify(eventRepository, never()).saveAndFlush(any());
    }

    @Test
    void submitEvent_success_callsAccountService() {
        when(eventRepository.findByEventId("e2")).thenReturn(Optional.empty());
        when(eventRepository.saveAndFlush(any(EventRecord.class))).thenAnswer(inv -> {
            EventRecord r = inv.getArgument(0);
            return r;
        });
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(ResponseEntity.ok(Map.of()));

        var req = new EventRequest("e2", "a2", "DEBIT", BigDecimal.TEN, "USD", "2023-01-01T00:00:00Z", Map.of());
        var resp = eventService.submitEvent(req);

        assertEquals("e2", resp.eventId());
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Map.class));
        verify(eventRepository, atLeastOnce()).saveAndFlush(any(EventRecord.class));
    }

    @Test
    void submitEvent_accountServiceFailure_marksEventFailedAndThrows() {
        when(eventRepository.findByEventId("e3")).thenReturn(Optional.empty());
        when(eventRepository.saveAndFlush(any(EventRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("service down"));

        EventRequest req = new EventRequest("e3", "a3", "CREDIT", BigDecimal.TEN, "USD", "2023-01-01T00:00:00Z", Map.of());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> eventService.submitEvent(req));
        assertEquals("Account service unavailable", exception.getMessage());

        ArgumentCaptor<EventRecord> captor = ArgumentCaptor.forClass(EventRecord.class);
        verify(eventRepository, times(1)).saveAndFlush(any(EventRecord.class));
        verify(eventRepository, times(1)).save(captor.capture());
        assertEquals("FAILED", captor.getValue().getStatus());
    }

    @Test
    void applyInAccountService_sendsExpectedPayload() {
        EventRecord record = new EventRecord("e4", "a4", "DEBIT", BigDecimal.TEN, "USD", java.time.Instant.parse("2026-05-15T14:02:11Z"), Map.of());

        eventService.applyInAccountService(record);

        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(restTemplate).postForEntity(anyString(), any(), eq(Map.class));
        verify(restTemplate).postForEntity(anyString(), argThat(request -> {
            Object body = ((org.springframework.http.HttpEntity<?>) request).getBody();
            return body instanceof Map && ((Map<?, ?>) body).get("eventId").equals("e4");
        }), eq(Map.class));
    }
}
