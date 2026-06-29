# Copilot Instructions - toggle-server

Operational instructions for AI-assisted contributions in this repository.

## Project Context

- Stack: Java 25, Spring Boot 3.5, Maven, JPA, Flyway.
- Main domains: toggle, client, delivery.
- Architecture: web -> application -> persistence/domain.

## Priorities

1. Functional correctness and contract preservation.
2. Small, traceable changes.
3. Testability and readability.
4. Consistency with existing contribution rules.

## Mandatory Guardrails

1. Always add or adjust tests when behavior changes.
2. Never change existing Flyway migrations in `src/main/resources/db/migration`.
3. Preserve ProblemDetail-based error handling.
4. Avoid cross-context coupling between toggle, client, and delivery.
5. Avoid N+1 patterns in potentially expensive queries.

## Implementation Conventions

- Prefer constructor injection.
- Avoid broad refactoring without functional need.
- Keep public APIs stable unless explicitly requested otherwise.
- In asynchronous changes, ensure explicit executor configuration when applicable.

## Review Rules

Before completing any task:
1. Run `mvn clean verify`.
2. Report changed files and residual risks.
3. Confirm there was no API contract break.
4. Confirm the rules in this file and `.instructions.md` were followed.

## When To Ask For Human Decision

- API contract change.
- Architecture trade-off between contexts.
- Sensitive security change.
- Ambiguous behavior not covered by tests or requirements.
