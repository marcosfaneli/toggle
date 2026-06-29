# Architecture - toggle-server

## Goal

`toggle-server` manages toggle definitions, client registration, and callback-based update delivery.

## Domain Contexts

### 1. toggle
Responsible for:
- Creating, reading, listing, and updating toggles.
- Validating toggle type and value.

Relevant packages:
- `src/main/java/com/toggle/server/toggle/web`
- `src/main/java/com/toggle/server/toggle/application`
- `src/main/java/com/toggle/server/toggle/persistence`

### 2. client
Responsible for:
- Client instance registration and removal.
- Heartbeat and instance lifecycle.
- Toggle subscriptions by client.

Relevant packages:
- `src/main/java/com/toggle/server/client/web`
- `src/main/java/com/toggle/server/client/application`
- `src/main/java/com/toggle/server/client/persistence`

### 3. delivery
Responsible for:
- Reacting to toggle updates.
- Delivering events to subscribed client callbacks.
- Recording delivery synchronization state.

Relevant packages:
- `src/main/java/com/toggle/server/delivery/application`
- `src/main/java/com/toggle/server/delivery/infrastructure`
- `src/main/java/com/toggle/server/delivery/domain`

### 4. shared
Responsible for:
- Shared infrastructure, error, and time utilities and types.

Relevant packages:
- `src/main/java/com/toggle/server/shared`

## Main Flow

1. A client calls an HTTP endpoint in the web layer.
2. The controller maps the request/command and delegates to an application-layer use case.
3. The use case applies business rules and persists or queries through persistence adapters.
4. On toggle update, a domain event triggers the delivery flow.
5. Delivery notifies subscribed callbacks and updates synchronization state.

## Boundaries And Dependencies

- Rule: web depends on application.
- Rule: application depends on contracts/domain and required adapters.
- Rule: persistence/infrastructure does not define business rules.
- Rule: avoid direct coupling between contexts without an explicit contract.

## Data And Migrations

- The database is managed by Flyway in `src/main/resources/db/migration`.
- Existing migrations are immutable.
- Schema evolution must be implemented through new `Vxxx__*.sql` versions.

## Errors And HTTP Contracts

- HTTP errors must follow ProblemDetail.
- Input validation stays in the web layer (Bean Validation) with consistent mapping.

## Tests

- Behavior changes must include tests.
- Web tests: `@WebMvcTest` for endpoint contracts.
- Business rules: unit tests with JUnit 5 and Mockito.
- Persistence/event changes should prioritize expanded integration coverage.

## Non-Negotiable Decisions

1. Do not change existing Flyway migrations.
2. Do not introduce unnecessary cross-context coupling.
3. Do not break public contracts without explicit alignment.
4. Do not finish a task without build/test validation.
