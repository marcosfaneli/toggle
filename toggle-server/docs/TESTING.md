# Testing Guide - toggle-server

Objective guide for keeping AI-assisted contributions reliable.

## Test Pyramid

1. Unit tests (majority)
- Target: use cases, mappers, and pure rules.
- Tools: JUnit 5 + Mockito.

2. Web slice
- Target: HTTP contract (status, payload, validation).
- Tool: `@WebMvcTest` with mocked dependencies.

3. Integration tests (progressive growth)
- Target: persistence, events, and end-to-end delivery flow.
- Recommendation: `@SpringBootTest` + consistent test database.

## When To Write Each Test

- Business rule changed: unit test required.
- Request/response/validation changed: `@WebMvcTest` required.
- Repository/query/event changed: include or expand integration tests.

## Minimum Scenario Pattern

For each behavior change, cover:
1. Happy path.
2. Invalid input.
3. Business rule violation.
4. External dependency error when applicable.

## Conventions

- Descriptive, behavior-oriented test names.
- Clear Arrange/Act/Assert.
- One failure reason per test.

## Test Definition Of Done

- All local tests pass with `mvn clean verify`.
- No flaky test introduced.
- Coverage for new critical paths added.
