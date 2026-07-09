# Authentication And Access Control Implementation Plan

This document describes the planned implementation path for adding access
control to Switchboard while keeping the project friendly for open-source
self-hosting.

The chosen direction is:

- OIDC/Keycloak for human access through the admin UI.
- API keys for service/application access through the Spring client library.
- Reverse proxy authentication is intentionally postponed.

No implementation is included here. This plan is meant to be executed in small
parts across future development sessions.

## 1. Goals

- Protect the admin UI and administrative APIs with OIDC.
- Support Keycloak in local Docker Compose for validation.
- Keep a simple local development mode without authentication.
- Add service API keys for client registration, heartbeat, deregistration, and
  remote toggle reads.
- Keep human identities and service identities separate.
- Define a foundation that can later support more auth modes without rewriting
  the application.

## 2. Non-Goals For The First Iteration

- No reverse proxy authentication mode.
- No local username/password authentication.
- No full organization/workspace model yet.
- No complex policy engine.
- No mTLS for service-to-server authentication.

## 3. Authentication Modes

Add a server-level auth mode configuration:

```yaml
toggle:
  auth:
    mode: none # none | oidc
```

Initial behavior:

- `none`: preserves current local-development behavior and leaves endpoints
  open.
- `oidc`: enables JWT validation for admin/human endpoints.

Future modes can be added later:

- `local`: database-backed local users.
- `proxy`: trusted headers from an external auth proxy.

## 4. Access Model

Initial roles:

| Role | Intended Actor | Permissions |
| --- | --- | --- |
| `ADMIN` | Platform/admin user | Full administrative access, including API key management |
| `MAINTAINER` | Developer/team maintainer | Create and update toggles, view clients |
| `VIEWER` | Read-only user | View toggles and clients |
| `SERVICE` | Application/client library | Register, heartbeat, deregister, and read assigned toggles |

Important rule:

- Human users authenticate with OIDC.
- Services authenticate with API keys.
- A service API key must not grant access to the admin UI.
- A human JWT must not be used as the normal credential for service heartbeat
  or registration.

## 5. Docker Compose And Keycloak

Add Keycloak to `docker-compose.yml` for local validation.

Recommended local ports:

| Component | URL |
| --- | --- |
| Admin UI | `http://localhost:4200` |
| Toggle Server | `http://localhost:8080` |
| Keycloak | `http://localhost:8089` |
| Adminer | `http://localhost:8081` |

Recommended Keycloak service:

- Use the official Keycloak container image.
- Run in development mode.
- Import a local realm file on startup.
- Expose Keycloak on `8089` to avoid conflicting with the toggle server.

Recommended realm file:

```text
docker/keycloak/switchboard-realm.json
```

Realm contents:

- Realm: `switchboard`
- Public SPA client: `switchboard-admin-ui`
- Valid redirect URIs:
  - `http://localhost:4200/*`
- Valid web origins:
  - `http://localhost:4200`
- Roles:
  - `switchboard-admin`
  - `switchboard-maintainer`
  - `switchboard-viewer`
- Development users:
  - `admin` with role `switchboard-admin`
  - `maintainer` with role `switchboard-maintainer`
  - `viewer` with role `switchboard-viewer`

Recommended validation:

- `docker compose up --build`
- Access Keycloak at `http://localhost:8089`.
- Confirm realm, client, roles, and test users are imported.
- Confirm the admin UI can redirect to Keycloak and receive a token.

## 6. Backend: OIDC Resource Server

Add Spring Security to `toggle-server`.

Expected dependencies:

- `spring-boot-starter-security`
- `spring-boot-starter-oauth2-resource-server`

Expected configuration for local Keycloak:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8089/realms/switchboard
```

The server must map Keycloak roles to Spring authorities:

| Keycloak Role | Internal Authority |
| --- | --- |
| `switchboard-admin` | `ROLE_ADMIN` |
| `switchboard-maintainer` | `ROLE_MAINTAINER` |
| `switchboard-viewer` | `ROLE_VIEWER` |

The mapper should support Keycloak role claims from typical locations such as:

- `realm_access.roles`
- `resource_access.<client-id>.roles`

## 7. Backend: Endpoint Authorization

When `toggle.auth.mode=oidc`, protect administrative endpoints:

| Endpoint | Required Roles |
| --- | --- |
| `GET /toggles` | `ADMIN`, `MAINTAINER`, `VIEWER` |
| `GET /toggles/{name}` | `ADMIN`, `MAINTAINER`, `VIEWER` |
| `GET /toggles/{name}/clients` | `ADMIN`, `MAINTAINER`, `VIEWER` |
| `POST /toggles` | `ADMIN`, `MAINTAINER` |
| `PATCH /toggles/{name}` | `ADMIN`, `MAINTAINER` |
| `GET /clients` | `ADMIN`, `MAINTAINER`, `VIEWER` |

Operational service endpoints should be protected separately by API key:

| Endpoint | Auth Type |
| --- | --- |
| `POST /clients/register` | API key |
| `POST /clients/heartbeat` | API key |
| `DELETE /clients/register/{instanceId}` | API key |
| Service toggle read endpoint | API key |

OpenAPI/Swagger behavior can remain disabled by default. In development, decide
whether Swagger should remain public, be protected, or be disabled when OIDC is
active.

## 8. Backend: API Keys For Services

Add persistent API key support for service authentication.

Recommended table:

```text
service_api_key
- id
- public_id
- service_name
- name
- key_hash
- status
- created_at
- last_used_at
- revoked_at
```

Recommended statuses:

```text
ACTIVE
REVOKED
```

Recommended behavior:

- Generate a high-entropy API key.
- Store only a secure hash of the key.
- Display the raw key only once at creation time.
- Require `X-API-Key` for service endpoints.
- Bind each key to a single `serviceName`.
- Reject registration/heartbeat/deregistration attempts where the request
  `serviceName` does not match the key owner.
- Update `last_used_at` after successful authentication.

Recommended service header:

```http
X-API-Key: <service-api-key>
```

Future option:

- Support OAuth2 client credentials for services later, but do not implement it
  in this first iteration.

## 9. Backend: API Key Management Endpoints

Add administrative endpoints for API key management.

Recommended routes:

```text
GET    /service-api-keys
POST   /service-api-keys
DELETE /service-api-keys/{publicId}
```

Recommended authorization:

| Endpoint | Required Role |
| --- | --- |
| `GET /service-api-keys` | `ADMIN` |
| `POST /service-api-keys` | `ADMIN` |
| `DELETE /service-api-keys/{publicId}` | `ADMIN` |

Creation response should include the raw key once:

```json
{
  "id": "01...",
  "serviceName": "checkout-service",
  "name": "local-dev",
  "apiKey": "swb_...",
  "createdAt": "2026-07-05T12:00:00Z"
}
```

List responses must not include the raw key.

## 10. Spring Client Library Changes

Add API key configuration to the Spring client starter:

```yaml
feature:
  toggles:
    api-key: ${SWITCHBOARD_API_KEY}
```

Update the HTTP client to send the key on service calls:

- register
- heartbeat
- deregister
- remote toggle reads

Expected header:

```http
X-API-Key: <service-api-key>
```

Validation:

- Existing clients should fail clearly if server API key auth is enabled but no
  API key is configured.
- Local `toggle.auth.mode=none` can continue working without API keys during
  development if desired.

## 11. Admin UI OIDC Integration

Integrate the Angular admin UI with Keycloak using Authorization Code + PKCE.

Expected UI behavior:

- Unauthenticated users are redirected to Keycloak.
- Authenticated users return to the admin UI.
- API calls include `Authorization: Bearer <access-token>`.
- Logout redirects through Keycloak.
- User identity is visible in the app chrome.
- UI actions are hidden or disabled based on roles.

Important rule:

- Role checks in the UI are convenience only.
- The backend remains the source of truth for authorization.

Recommended Angular additions:

- `AuthService`
- `authGuard`
- HTTP interceptor for Bearer tokens
- role helper utility
- login/logout controls

Protected routes:

```text
/toggles
/toggles/new
/toggles/:name/edit
/clients
/service-api-keys
```

## 12. Admin UI API Key Management

Add an admin-only API key management screen after OIDC is working.

Recommended route:

```text
/service-api-keys
```

Expected features:

- List service API keys.
- Create a new key for a service.
- Show the generated key once after creation.
- Revoke an existing key.
- Show status and last-used timestamp.

Only users with `ADMIN` should see this navigation item.

## 13. Testing Plan

Backend tests:

- `toggle.auth.mode=none` preserves current behavior.
- Missing Bearer token returns `401` for admin endpoints in OIDC mode.
- Invalid Bearer token returns `401`.
- Valid token without required role returns `403`.
- `VIEWER` can read but cannot create/update.
- `MAINTAINER` can create/update toggles.
- `ADMIN` can manage service API keys.
- Missing API key returns `401` for service endpoints when API key auth is
  active.
- Invalid API key returns `401`.
- Revoked API key returns `401`.
- API key for `service-a` cannot register heartbeat for `service-b`.

Frontend tests:

- Protected routes require authentication.
- HTTP interceptor attaches Bearer token.
- Logout clears session.
- Viewer users do not see create/edit controls.
- Admin users see API key management.

Manual Docker Compose validation:

- Start the full stack with Keycloak.
- Log in as `admin` and create a toggle.
- Log in as `maintainer` and update a toggle.
- Log in as `viewer` and confirm read-only behavior.
- Create a service API key as admin.
- Start a Spring client with the generated API key.
- Confirm registration and heartbeat work.
- Confirm invalid API key is rejected.

## 14. Suggested Implementation Order

1. Add Keycloak to Docker Compose and create the importable realm.
2. Add backend auth configuration with `toggle.auth.mode=none|oidc`.
3. Add Spring Security resource server support and JWT role mapping.
4. Protect administrative endpoints by role.
5. Integrate admin UI login with Keycloak.
6. Add Bearer token HTTP interceptor and route guards in Angular.
7. Add service API key persistence and authentication filter.
8. Protect service endpoints with API key authentication.
9. Update the Spring client starter to send `X-API-Key`.
10. Add API key management endpoints.
11. Add admin UI screen for API key management.
12. Update README and local development documentation.

## 15. First Milestone Acceptance Criteria

The first useful milestone should prove human access control end to end:

- `docker compose up --build` starts MySQL, server, admin UI, and Keycloak.
- Keycloak imports the `switchboard` realm automatically.
- Admin UI redirects unauthenticated users to Keycloak.
- Admin UI sends Bearer tokens to the server.
- Server validates Keycloak JWTs.
- `admin` and `maintainer` can create/update toggles.
- `viewer` can view toggles but cannot create/update them.
- `toggle.auth.mode=none` still supports the simple local development flow.

## 16. Second Milestone Acceptance Criteria

The second milestone should prove service authentication end to end:

- Admin can create and revoke service API keys.
- Raw API key is shown only once.
- Keys are stored hashed.
- Spring client starter can send `X-API-Key`.
- Services can register, heartbeat, deregister, and read toggles with a valid
  key.
- Invalid, revoked, or mismatched-service keys are rejected.

