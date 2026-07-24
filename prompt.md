# Event Ledger Project Prompt

This repository contains the Event Ledger microservice system with two Spring Boot 3.x services:

- `event-gateway`
- `account-service`

The system handles sensitive financial ledger transactions and requires strict idempotency, distributed tracing, out-of-order event tolerance, and resilient downstream behavior.

## Purpose

Use this prompt file to provide context to AI assistants for code generation, review, and test scaffolding.

## Key Facts

- Java 17 and Spring Boot 3.x
- Spring Cloud and Resilience4j
- H2 database for local/testing usage
- Event-driven ledger semantics
- Critical focus areas:
  - idempotent event handling
  - concurrent balance updates
  - circuit breaker behavior
  - tracing header propagation
  - secure input validation

## AI Usage Guidance

When generating code or tests, follow these principles:

- Generate boilerplate, DTOs, controllers, and test scaffolding only.
- Do not implement core financial business rules without human validation.
- Avoid using proprietary or production data in prompts.
- Keep requests specific and scoped to a single class, endpoint, or test.

## Recommended Prompt Patterns

1. Idempotent endpoint scaffold

```
Write Spring Boot 3.x Java code for a REST endpoint in the `event-gateway` service that accepts a POST /events request.
The endpoint should accept eventId, accountId, type, amount, currency, eventTimestamp, metadata and validate all fields.
Generate the controller class and request/response DTOs only. Do not implement ledger balance updates or persistence logic.
```

2. Resilience4j client integration

```
Generate Spring Boot 3.x configuration and Java service code for calling the `account-service` from `event-gateway`.
Use RestTemplate or WebClient with Resilience4j circuit breaker and propagate tracing headers.
Provide only the service class and configuration snippet.
```

3. Integration test scaffolding

```
Create a JUnit 5 Spring Boot integration test for the `account-service`.
Use @SpringBootTest(webEnvironment = RANDOM_PORT) and TestRestTemplate.
Verify that POST /accounts/{accountId}/transactions succeeds and GET /accounts/{accountId} returns the expected balance.
```

## Review Requirements

- Human review is mandatory for AI-generated code.
- Focus review on idempotency, race conditions, tracing, and fallback logic.
- Do not merge AI output without explicit peer approval.

## Security and Compliance

- Any AI-generated code must be scanned by approved security tools before merge.
- Do not introduce unvetted dependencies or insecure coding patterns.
- Maintain auditability by documenting AI-assisted changes in PR descriptions.
