package com.example.eventgateway.controller;

import com.example.eventgateway.dto.ApiResponse;
import com.example.eventgateway.dto.EventRequest;
import com.example.eventgateway.dto.EventResponse;
import com.example.eventgateway.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventControllerTest {

    private EventService eventService;
    private EventController controller;

    @BeforeEach
    void setUp() {
        eventService = mock(EventService.class);
        controller = new EventController(eventService);
    }

    @Test
    void submitEvent_shouldReturnCreatedWhenSuccessful() {
        EventRequest request = new EventRequest("evt-1", "acct-1", "CREDIT", new BigDecimal("100"), "USD", "2026-05-15T14:02:11Z", Map.of());
        EventResponse response = new EventResponse(1L, "evt-1", "acct-1", "CREDIT", new BigDecimal("100"), "USD", java.time.Instant.parse("2026-05-15T14:02:11Z"), "APPLIED", Map.of());

        when(eventService.submitEvent(request)).thenReturn(response);

        ResponseEntity<?> result = controller.submitEvent(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertTrue(result.getBody() instanceof ApiResponse);
        ApiResponse<?> apiResponse = (ApiResponse<?>) result.getBody();
        assertEquals("SUCCESS", apiResponse.status());
        assertEquals(response, apiResponse.data());
    }

    @Test
    void submitEvent_shouldReturnServiceUnavailableWhenAccountServiceIsDown() {
        EventRequest request = new EventRequest("evt-2", "acct-1", "DEBIT", new BigDecimal("20"), "USD", "2026-05-15T14:02:11Z", Map.of());

        when(eventService.submitEvent(request)).thenThrow(new IllegalStateException("Account service unavailable"));

        ResponseEntity<?> result = controller.submitEvent(request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, result.getStatusCode());
        assertTrue(result.getBody() instanceof ApiResponse);
        ApiResponse<?> apiResponse = (ApiResponse<?>) result.getBody();
        assertEquals("ERROR", apiResponse.status());
        assertEquals("Account service unavailable", apiResponse.message());
    }

    @Test
    void getEvent_shouldReturnEventWhenPresent() {
        EventResponse response = new EventResponse(1L, "evt-1", "acct-1", "CREDIT", new BigDecimal("100"), "USD", java.time.Instant.parse("2026-05-15T14:02:11Z"), "APPLIED", Map.of());
        when(eventService.getEventById(1L)).thenReturn(Optional.of(response));

        ResponseEntity<ApiResponse<EventResponse>> result = controller.getEvent(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("SUCCESS", result.getBody().status());
        assertEquals(response, result.getBody().data());
    }

    @Test
    void getEvent_shouldReturnNotFoundWhenMissing() {
        when(eventService.getEventById(99L)).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<EventResponse>> result = controller.getEvent(99L);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertEquals("ERROR", result.getBody().status());
        assertEquals("Event not found", result.getBody().message());
    }

    @Test
    void listEvents_shouldReturnEventsForAccount() {
        EventResponse first = new EventResponse(1L, "evt-1", "acct-1", "CREDIT", new BigDecimal("100"), "USD", java.time.Instant.parse("2026-05-15T14:02:11Z"), "APPLIED", Map.of());
        EventResponse second = new EventResponse(2L, "evt-2", "acct-1", "DEBIT", new BigDecimal("20"), "USD", java.time.Instant.parse("2026-05-15T15:02:11Z"), "APPLIED", Map.of());
        when(eventService.listEventsByAccount("acct-1")).thenReturn(List.of(first, second));

        ResponseEntity<ApiResponse<List<EventResponse>>> result = controller.listEvents("acct-1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().data().size());
        assertEquals("evt-1", result.getBody().data().get(0).eventId());
    }

    @Test
    void health_shouldReturnUp() {
        ResponseEntity<ApiResponse<String>> result = controller.health();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("UP", result.getBody().data());
    }
}
