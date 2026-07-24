package com.example.eventgateway.controller;

import com.example.eventgateway.dto.ApiResponse;
import com.example.eventgateway.dto.EventRequest;
import com.example.eventgateway.dto.EventResponse;
import com.example.eventgateway.service.EventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/events")
@Validated
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<?> submitEvent(
            @Valid @RequestBody EventRequest request) {
        try {
            EventResponse response = eventService.submitEvent(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("X-Trace-Id", MDC.get("traceId"))
                    .body(ApiResponse.success(response, MDC.get("traceId")));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header("X-Trace-Id", MDC.get("traceId"))
                    .body(ApiResponse.failure("Account service unavailable", MDC.get("traceId")));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(@PathVariable Long id) {
        Optional<EventResponse> event = eventService.getEventById(id);
        if (event.isPresent()) {
            return ResponseEntity.ok()
                    .header("X-Trace-Id", MDC.get("traceId"))
                    .body(ApiResponse.success(event.get(), MDC.get("traceId")));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .header("X-Trace-Id", MDC.get("traceId"))
                .body(ApiResponse.failure("Event not found", MDC.get("traceId")));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EventResponse>>> listEvents(@RequestParam("account") String accountId) {
        List<EventResponse> events = eventService.listEventsByAccount(accountId);
        return ResponseEntity.ok()
                .header("X-Trace-Id", MDC.get("traceId"))
                .body(ApiResponse.success(events, MDC.get("traceId")));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok()
                .header("X-Trace-Id", MDC.get("traceId"))
                .body(ApiResponse.success("UP", MDC.get("traceId")));
    }
}
