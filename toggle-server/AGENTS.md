# AGENTS Charter - toggle-server

This file defines how AI agents should collaborate in this repository.
Goal: reduce ambiguity, maintain technical quality, and avoid out-of-scope changes.

## Scope

Applies to the whole repository.
Complements the rules in `.instructions.md` and `CONTRIBUTING.md`.

## Roles

### 1) Planner
- Understands the request and defines the technical scope.
- Identifies impact on API, database, tests, and observability.
- Defines a short execution plan with objective validation.

### 2) Implementer
- Makes small, focused changes.
- Preserves public contracts unless explicitly requested otherwise.
- Adds or updates tests for every behavior change.
- Does not change existing Flyway migrations in `src/main/resources/db/migration`.

### 3) Reviewer
- Checks for functional regressions, contract breaks, and operational risk.
- Checks adherence to the context-based architecture (toggle, client, delivery).
- Confirms the Definition of Done before closing.

## Handoff Between Roles

1. Planner -> Implementer
- Delivers: goal, target files, acceptance criteria, and test strategy.

2. Implementer -> Reviewer
- Delivers: focused diff, risks, validation commands executed, and results.

3. Reviewer -> Closure
- Delivers: final assessment (approved or blocked) with objective pending items.

## Non-Negotiable Rules

1. Every behavior change requires a test.
2. Do not edit an existing Flyway migration.
3. Avoid cross-context coupling between toggle, client, and delivery.
4. Keep the ProblemDetail error standard.
5. Run build/test verification before finishing.

## Human Escalation

Escalate when any of the following happens:
- API contract changes are required (request/response/status).
- Existing data model changes are required without a clear migration.
- Architecture rules conflict with the functional request.
- Security or compliance risk exists.

## Definition Of Done

- Build and tests pass.
- No known functional regression.
- Documentation is updated when behavior changes.
- The change is the minimum necessary to resolve the request.
