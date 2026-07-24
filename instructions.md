# AI Usage Instructions for Event Ledger

This repository uses generative AI tools in a controlled way for the Event Ledger microservice system.

## Purpose

Provide explicit instructions for team members and AI assistants about how to safely generate and review code for the `event-gateway` and `account-service` services.

## What AI Can Do

- Generate Spring Boot 3.x boilerplate for controllers, DTOs, and configuration.
- Scaffold JUnit 5 integration tests and test harness code.
- Create standard Resilience4j setup patterns.
- Generate request validation annotations and mapping classes.
- Generate common utility classes that do not contain business-critical ledger rules.

## What AI Must Not Do

- Implement or validate core financial business logic.
- Decide idempotency semantics for event deduplication.
- Implement concurrent balance update correctness without human review.
- Create production database schemas using live or proprietary data.
- Auto-commit code or bypass peer review.

## Review Requirements

- Every AI-generated change must be reviewed and approved by a qualified engineer.
- Review focus areas:
  - idempotency key handling
  - concurrent balance calculations
  - distributed tracing header propagation
  - circuit breaker and fallback behavior
  - input validation for financial payloads
- Document AI involvement in PR descriptions as `AI-assisted: GitHub Copilot`.

## Security and Compliance

- Run security scans on AI-generated code before merging.
- Use approved tools such as SonarQube, Snyk, or equivalent.
- Validate dependency licensing when AI suggests new libraries.
- Do not accept AI output that contains sensitive or proprietary business data.

## Prompt Best Practices

When interacting with AI, use specific, scoped prompts such as:

- `Generate a Spring Boot 3.x controller for POST /events with validation and DTOs, without implementing balance calculation logic.`
- `Create a JUnit 5 integration test for /accounts/{accountId}/transactions using TestRestTemplate.`
- `Generate Resilience4j circuit breaker configuration for a downstream call from event-gateway to account-service.`

## Document Purpose

This `instructions.md` file is intended as a concise companion to `prompt.md` and should be referenced when using AI tools for this repository.
