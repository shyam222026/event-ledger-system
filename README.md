# Event Ledger System

## Architecture Overview

This project implements a small event-driven ledger platform with two independently deployable Spring Boot services:

- `event-gateway`
  - Acts as the ingress service for transaction events.
  - Validates and persists incoming events in its own H2 database.
  - Dispatches processed events to `account-service` using a Resilience4j circuit breaker.
  - Propagates tracing headers from inbound requests to outbound calls for distributed tracing.
  - Exposes a health endpoint at `/events/health`.

- `account-service`
  - Handles account state and transaction history.
  - Stores transactions and calculates account balances dynamically.
  - Ensures idempotency by deduplicating events using `eventId`.
  - Exposes account endpoints under `/accounts` and a service health endpoint at `/accounts/health`.

The gateway and account service interact through HTTP: the gateway forwards validated transaction events to the account service, and the account service applies the transaction to account state.

## Setup Instructions

### Prerequisites

- Java 17 or newer
- Maven 3.8+ installed
- Docker and Docker Compose (optional, for containerized startup)

### Install Dependencies

From the repository root:

```bash
mvn -pl event-gateway,account-service -am test
```

This command will build both modules and run the integration tests.

## Starting the Services

### Option 1: Manual Startup

Start `account-service` first:

```bash
cd account-service
mvn spring-boot:run
```

Then start `event-gateway` in a separate terminal:

```bash
cd ../event-gateway
mvn spring-boot:run
```

### Option 2: Docker Compose

The repository includes a `docker-compose.yml` for local startup. It runs both services in containers.

```bash
docker-compose up --build
```

> Note: The current `docker-compose.yml` launches each service with `./mvnw`. If the Maven wrapper is not present in the project, replace those commands with `mvn` or run the services manually.

## Running Tests

From the repository root:

```bash
mvn -pl event-gateway,account-service -am test
```

This executes the integration tests for both services.

## Code Coverage

JaCoCo coverage reports are generated during the normal Maven test lifecycle.
The build also enforces minimum coverage thresholds during `verify`, and each module can declare its own threshold values.

From the repository root:

```bash
mvn -pl event-gateway,account-service -am test
```

Then open the generated report for each module:

```bash
open event-gateway/target/site/jacoco/index.html
open account-service/target/site/jacoco/index.html
```

If `open` is not available, open the HTML files directly in your browser.

## Resiliency Pattern

The system uses a gateway-side circuit breaker pattern via Resilience4j.

- The gateway protects the account service call with a circuit breaker.
- If the account service becomes unavailable, the gateway fails fast and returns a service unavailable response.
- This prevents cascading failures and avoids long blocking retries when downstream capacity is lost.

The account service also uses idempotent processing for event handling, ensuring repeated delivery of the same event does not duplicate account updates.

## Notes

- The services are configured to use H2 for development and testing.
- Health checks are available at:
  - `http://localhost:8081/events/health`
  - `http://localhost:8082/accounts/health`
