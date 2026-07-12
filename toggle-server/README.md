# Toggle Server

Feature toggle service for client registration, toggle updates, and callback-based change delivery.

## Stack
- Java 25
- Spring Boot 3.5
- Spring Data JPA
- Flyway
- MySQL (production) / H2 (tests)
- Maven

## Requirements
- JDK 25
- Maven 3.9+

In this workspace's local environment:

```bash
export JAVA_HOME=/home/faneli/.jdk/jdk-25.0.2
export PATH=/home/faneli/.jdk/jdk-25.0.2/bin:$PATH
```

## Run Locally

```bash
mvn spring-boot:run
```

To run with OIDC and keep Swagger enabled locally, prefer explicit Springdoc flags:

```bash
SPRING_PROFILES_ACTIVE=dev \
SPRINGDOC_API_DOCS_ENABLED=true \
SPRINGDOC_SWAGGER_UI_ENABLED=true \
TOGGLE_AUTH_MODE=oidc \
OIDC_ISSUER_URI=http://localhost:8089/realms/switchboard \
OIDC_JWK_SET_URI=http://localhost:8089/realms/switchboard/protocol/openid-connect/certs \
OIDC_CLIENT_ID=switchboard-admin-ui \
DB_HOST=localhost \
DB_PORT=3306 \
DB_NAME=toggle_db \
DB_USERNAME=toggle \
DB_PASSWORD=toggle \
DB_USE_SSL=false \
DB_ALLOW_PUBLIC_KEY_RETRIEVAL=true \
TOGGLE_SECURITY_ALLOW_LOCAL_CALLBACKS=true \
mvn spring-boot:run
```

## API Documentation

With the application running, open:

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

If `/swagger-ui/index.html` returns `No static resource`, the runtime most likely started with Springdoc disabled. Confirm that either the `dev` profile is active or both `SPRINGDOC_API_DOCS_ENABLED=true` and `SPRINGDOC_SWAGGER_UI_ENABLED=true` are set.

### Swagger Authentication

Swagger UI supports two auth options against the local Keycloak realm:

1. Open Swagger UI and click `Authorize`.
2. Choose one of the schemes:
	- `oidc`: Authorization Code + PKCE (browser redirect login)
	- `oidc-password`: direct username/password token flow (local/dev)
3. For `oidc-password`, use:
	- client id: `switchboard-admin-ui`
	- username/password from the seeded local users
4. Authenticate with one of the seeded local users:
	- `admin` / `admin`
	- `maintainer` / `maintainer`
	- `viewer` / `viewer`

This authentication is intended for the human/admin endpoints protected by Bearer token. Service endpoints that use `X-API-Key` keep their existing flow outside Swagger login.

### Clients

```http
GET /clients
GET /clients?serviceName=checkout-service
GET /clients?status=ACTIVE
GET /clients?serviceName=checkout-service&status=INACTIVE
```

Lists registered client instances and their toggle subscriptions. The
`serviceName` and `status` parameters are optional; when provided, they filter
the result by the requested service and status. Accepted `status` values:
`ACTIVE` and `INACTIVE`.

## Build And Tests

```bash
mvn clean verify
```

The command above runs:
- compilation
- tests
- JaCoCo coverage report
- coverage gate (initial minimum of 30% line coverage for the bundle)

## Docker

```bash
docker build -t toggle-server:local .
```

## Configuration
The main variables are defined in `src/main/resources/application.yml` and include:
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- `TOGGLE_AUTH_MODE` (`none` or `oidc`)
- `OIDC_ISSUER_URI`, `OIDC_JWK_SET_URI`, `OIDC_CLIENT_ID`
- `toggle.api.max-page-size`
- `toggle.heartbeat.*`
- `toggle.delivery.timeout-ms`

In `oidc` mode, human/admin endpoints require a Bearer token. Service endpoints
use `X-API-Key`, and service keys are managed through `/service-api-keys` by
users with the `ADMIN` role.

## Database
Migrations are in `src/main/resources/db/migration` and use Flyway versioning
(`Vxxx__...sql`). Never change already-applied migrations; always create a new
version.

## CI
The pipeline in `.github/workflows/build.yml` runs `mvn clean verify` for pushes
and pull requests.

## Agentic Coding
Main documents for AI-assisted contributions:
- `AGENTS.md`
- `.github/copilot-instructions.md`
- `.instructions.md`
- `docs/ARCHITECTURE.md`
- `docs/CODE_PATTERNS.md`
- `docs/TESTING.md`
