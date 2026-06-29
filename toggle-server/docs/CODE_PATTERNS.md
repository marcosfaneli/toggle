# Code Patterns - toggle-server

This document defines implementation patterns to reduce variation in AI-assisted contributions.

## 1. New HTTP Endpoint

Checklist:
1. Create or adjust input and output DTOs in the context's web package.
2. Apply Bean Validation to the request.
3. Map the request to an application command.
4. Delegate business rules to a use case.
5. Return status and payload consistently with the existing contract.
6. Cover success and error cases with `@WebMvcTest`.

Expected structure:
- web: Controller + CommandMapper
- application: UseCase + Command/Result
- domain/persistence: only what is necessary

## 2. New Use Case

Checklist:
1. Class in the context's application package.
2. Action-oriented name, for example `CreateXUseCase` or `UpdateYUseCase`.
3. Explicit constructor with dependencies.
4. Business-rule validation inside the use case.
5. No HTTP logic in the application layer.
6. Unit tests for the happy path and error rules.

Simplified template:

```java
@Service
public class ExampleUseCase {

    private final ExampleRepository repository;

    public ExampleUseCase(ExampleRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ExampleResult execute(ExampleCommand command) {
        // business rule
        return repository.save(command);
    }
}
```

## 3. New Persistence Adapter

Checklist:
1. Keep responsibilities limited to I/O and mapping.
2. Avoid business rules inside the adapter.
3. Prevent N+1 queries for collection lookups.
4. Keep queries index-friendly when possible.
5. Cover critical behavior with integration tests when applicable.

## 4. New Domain Or Delivery Event

Checklist:
1. The event must represent a clear business fact.
2. Asynchronous listeners need observable failure handling.
3. Do not block the request thread on external delivery.
4. Persist minimal synchronization state for troubleshooting.
5. Add a regression test for the delivery flow.

## 5. Minimum Change Rule

- Do not mix broad refactoring with functional changes.
- Avoid changing more than one context without explicit justification.
- When in doubt, split the work into smaller PRs.
