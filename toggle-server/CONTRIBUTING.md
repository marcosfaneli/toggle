# Contributing

## Contribution Rules
- Every change must include or update tests.
- Use `mvn clean verify` before opening a PR.
- Do not change existing Flyway migrations; add a new version.
- Keep input validation in the web layer (Bean Validation).
- Preserve standardized `ProblemDetail` error handling.

## Architecture Patterns
- Domain separated by contexts: `toggle`, `client`, `delivery`.
- Expected flow: web -> application -> persistence/domain.
- Avoid direct coupling between contexts; prefer explicit contracts when evolving the code.

## Pull Request Checklist
- [ ] Local build and tests passed (`mvn clean verify`)
- [ ] Error cases covered by tests
- [ ] No API contract break
- [ ] Database migration included when required
- [ ] Relevant logs kept for troubleshooting

## AI Contribution Guides
- `AGENTS.md`
- `.github/copilot-instructions.md`
- `docs/ARCHITECTURE.md`
- `docs/CODE_PATTERNS.md`
- `docs/TESTING.md`
