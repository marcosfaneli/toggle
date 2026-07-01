# Technical Specification - Feature Toggle Platform (MVP)

## 1. Overview

This specification defines the MVP for a Feature Toggle platform with:

- A central server service for toggle creation and distribution.
- A Spring client library for consuming toggles.
- Active synchronization through HTTP PUT callbacks from the server to each registered pod.
- A required client-defined consumption strategy (LOCAL_CACHE or REMOTE_ALWAYS).

Supporting documents:

- Flow: [feature-toggle-flow.puml](feature-toggle-flow.puml)
- Data model: [feature-toggle-data-model.puml](feature-toggle-data-model.puml)

---

## 2. MVP Goal

Deliver a simple, performant, and scalable mechanism to enable or disable features and optionally provide a simple typed value per toggle.

### 2.1 Main Requirements

- Centralized toggle management on the server.
- Registration of each client instance (pod) during startup.
- Distribution of updates to all consumer instances.
- Synchronization control per instance.
- Optional value support with simple types: STRING and NUMBER.

### 2.2 MVP Architecture Decisions

- The consumption strategy is the client's responsibility.
- The server does not govern strategy; it only delivers values and records delivery state.
- Delivery confirmation is based on the standard HTTP response from PUT (200 OK expected).

### 2.3 Execution Assumption (PoC)

- This project is treated as a proof of concept (PoC).
- Security requirements are not implemented in this phase.

---

## 3. Scope And Out Of Scope

### 3.1 MVP Scope

- Boolean toggle with optional value.
- Optional value as a child object, with `type` + `raw`.
- Per-instance pod registration with its own `callbackUrl`.
- Delivery retry when PUT fails by timeout or error.
- Per-instance synchronization with delivery status.

### 3.2 Out Of Scope For This MVP

- Toggle segmentation by user, region, or device.
- Percentage rollout rules.
- Central governance of strategy.
- Complex value types (JSON/ARRAY/OBJECT).
- Security (authentication, authorization, origin validation, hardening).
- Complete administrative UI.

---

## 4. Architecture And Topology

### 4.1 Components

- Feature Toggle Server:
  - Toggle CRUD.
  - Consumer instance registration.
  - Update distribution through PUT to each pod callback URL.
  - Per-instance synchronization status control.

- Feature Toggle Client Library (Spring):
  - Local configuration loading during startup.
  - Instance registration with the server.
  - Local storage (cache) when the strategy is LOCAL_CACHE.
  - Remote lookup when the strategy is REMOTE_ALWAYS.
  - Internal endpoint for update PUT callbacks.

- Relational database:
  - Persists toggles, instances, subscriptions, sync state, and delivery history.

### 4.2 Kubernetes Topology

- Each pod registers its own instance with the server.
- The server delivers by instance, not by Kubernetes Service.
- Each pod exposes an internal callback endpoint.

---

## 5. Server Implementation

### 5.1 Server Responsibilities

- Manage the toggle lifecycle.
- Persist and version toggle changes.
- Receive instance registration and deregistration.
- Distribute updates to consumer instances through PUT.
- Maintain per-instance synchronization status.

### 5.2 Server Data Model

Based on [feature-toggle-data-model.puml](feature-toggle-data-model.puml).

Main entities:

- FeatureToggle
  - BIGINT (PK), publicId (UUIDv7/ULID), name, maintainer, enabled, version, updatedAt.
  - `name` is globally unique.
  - `maintainer` identifies the team or person responsible for the toggle.

- FeatureToggleValue (0..1 per toggle)
  - Optional child object for typed value.
  - toggleId (FK + UNIQUE), valueType (STRING|NUMBER), valueRaw.

- ClientInstance
  - One row per active pod.
  - instanceId unique per serviceName.
  - callbackUrl per instance.

- ClientToggleSubscription
  - Links an instance to a consumed toggle.
  - consumeMode sent by the client.

- ToggleSyncState
  - State per toggle + instance.
  - SYNCED, PENDING_RESPONSE, OUT_OF_SYNC, INACTIVE.

- ToggleDeliveryEvent
  - History of HTTP callback attempts.

ID standard:

- Internal PK/FK: BIGINT.
- Public IDs: UUIDv7/ULID.

### 5.3 Server APIs

#### 5.3.0 Standard Error Contract

- All server error responses must use `application/problem+json` (ProblemDetail).
- Minimum required structure for every endpoint:
  - `status`: numeric HTTP status code.
  - `title`: stable summary of the error type.
  - `detail`: problem description.
  - `instance`: URI for the failed request.
- For uniqueness rules, the recommended implementation pattern is direct `insert` protected by a `UNIQUE` constraint, mapping integrity violations to business errors (409).
- Avoid `check-then-insert` to reduce race-condition windows in concurrent scenarios.

Business error example (409):

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Toggle 'new-checkout' already exists",
  "instance": "/toggles"
}
```

#### 5.3.1 Create Toggle

- Method: POST
- Route: /toggles

Request:

```json
{
  "name": "new-checkout",
  "maintainer": "checkout-service",
  "enabled": true,
  "value": {
    "type": "STRING",
    "raw": "v2"
  }
}
```

Notes:

- `value` is optional.
- When `value` exists, `type` is required and must be STRING or NUMBER.

Response 201:

```json
{
  "id": "01J...",
  "name": "new-checkout",
  "maintainer": "checkout-service",
  "enabled": true,
  "value": {
    "type": "STRING",
    "raw": "v2"
  },
  "version": 1,
  "updatedAt": "2026-04-21T10:00:00Z"
}
```

#### 5.3.2 Update Toggle

- Method: PATCH
- Route: /toggles/{name}

Request:

```json
{
  "enabled": false,
  "value": {
    "type": "NUMBER",
    "raw": "10"
  }
}
```

Rules:

- Increment `version` on every change.
- Trigger distribution to registered consumer instances.

#### 5.3.3 Query Toggles

- Method: GET
- Route: /toggles?maintainer={maintainer}

Response 200:

```json
{
  "toggles": [
    {
      "name": "new-checkout",
      "enabled": false,
      "value": {
        "type": "NUMBER",
        "raw": "10"
      },
      "version": 2,
      "updatedAt": "2026-04-21T10:10:00Z"
    }
  ]
}
```

#### 5.3.4 Query Maintainers

- Method: GET
- Route: /maintainers

Returns the distinct maintainers currently associated with toggles, sorted
alphabetically.

#### 5.3.4 Register Client Instance

- Method: POST
- Route: /clients/register

Request:

```json
{
  "serviceName": "checkout-service",
  "instanceId": "checkout-7d8d4c7f6f-abcde",
  "podName": "checkout-7d8d4c7f6f-abcde",
  "namespace": "payments",
  "callbackUrl": "http://10.42.1.25:8080/internal/feature-toggles",
  "subscriptions": [
    { "toggleName": "new-checkout", "consumeMode": "LOCAL_CACHE" },
    { "toggleName": "payment-v2", "consumeMode": "REMOTE_ALWAYS" }
  ]
}
```

Response 200:

```json
{
  "serviceName": "checkout-service",
  "instanceId": "checkout-7d8d4c7f6f-abcde",
  "toggles": [
    {
      "name": "new-checkout",
      "enabled": true,
      "value": { "type": "STRING", "raw": "v2" },
      "version": 3,
      "updatedAt": "2026-04-21T10:20:00Z"
    }
  ]
}
```

#### 5.3.5 Remove Instance Registration

- Method: DELETE
- Route: /clients/register/{instanceId}

Response 204.

#### 5.3.6 Heartbeat (Recommended)

- Method: POST
- Route: /clients/heartbeat

Request:

```json
{
  "instanceId": "checkout-7d8d4c7f6f-abcde",
  "timestamp": "2026-04-21T10:30:00Z"
}
```

Usage:

- Update `lastHeartbeatAt` and `lastSeenAt`.
- Avoid keeping records for dead pods.

### 5.4 Server Flows

#### 5.4.1 Toggle Change

1. Persist the new state/value and increment `version`.
2. Identify registered consumer instances.
3. For each instance, send PUT to `callbackUrl`.
4. If the response is 200, mark SYNCED.
5. If the call times out or fails, mark OUT_OF_SYNC and schedule retry.

### 5.5 Retry And Timeout (Server)

Initial suggestion:

- Callback PUT timeout: 2s.
- Retry with exponential backoff: 1s, 2s, 4s, 8s, 16s.
- Maximum attempts: 5.
- After the maximum, keep OUT_OF_SYNC and record the error.

### 5.6 Observability (Server)

Structured logs with minimum fields:

- toggleName
- instanceId
- version
- callbackUrl
- responseCode
- latencyMs
- syncStatus

Metrics:

- toggle_put_delivery_success_total
- toggle_put_delivery_failure_total
- toggle_put_delivery_latency_ms
- toggle_sync_out_of_sync_instances
- toggle_register_total
- toggle_register_active_instances

Tracing:

- Propagate correlationId/requestId from the server to the PUT callback.

### 5.7 Performance And Scalability (Server)

- Numeric PK/FK values (BIGINT) reduce join and index cost.
- Read operations should use `@Transactional(readOnly = true)`:
  - Hibernate does not snapshot loaded entities, reducing memory usage.
  - Dirty checking is disabled, avoiding current-state versus snapshot comparison during flush.
  - No flush occurs before queries, reducing database round trips.
  - Some datasources/drivers automatically route read-only transactions to read replicas.
- Recommended indexes:
  - FeatureToggle(name) UNIQUE
  - FeatureToggle(maintainer)
  - ClientInstance(serviceName, instanceId) UNIQUE
  - ToggleSyncState(toggleId, clientInstanceId) UNIQUE
  - ToggleSyncState(syncStatus)
  - ToggleDeliveryEvent(clientInstanceId, createdAt)
- Asynchronous distribution through an internal server delivery queue or worker.

---

## 6. Client Library Implementation (Spring)

### 6.1 Client Library Responsibilities

- Read local application configuration.
- Declare strategies for each consumed toggle.
- Register the instance during startup.
- Expose the internal callback PUT endpoint.
- Evaluate toggles at runtime with LOCAL_CACHE or REMOTE_ALWAYS.
- Update the local cache when an update is received.

### 6.2 Client Configuration

Example properties:

```yaml
feature:
  toggles:
    consumed:
      new-checkout: LOCAL_CACHE
      payment-v2: REMOTE_ALWAYS
    callback-url: /internal/feature-toggles
```

Rules:

- Every consumed toggle must have a strategy declared in the `consumed` map.
- `callback-url` represents the internal endpoint path in the application.
- During registration, the library must send the full `callbackUrl` resolved for the instance or pod.

### 6.3 Client Callback Endpoint

#### 6.3.1 Update Received From Server

- Method: PUT
- Route: /internal/feature-toggles/{toggleName}

Request:

```json
{
  "enabled": false,
  "value": {
    "type": "NUMBER",
    "raw": "10"
  },
  "version": 4,
  "updatedAt": "2026-04-21T10:35:00Z",
  "instanceId": "checkout-7d8d4c7f6f-abcde"
}
```

Expected response:

- 200 OK on success.
- 4xx/5xx on processing failure.

### 6.4 Client Library Rules

- Strategy is required for every consumed toggle.
- The server does not change the strategy sent by the client in the MVP.
- `value` is optional.
- If `value` exists, `type` is required (STRING or NUMBER) and `raw` is required.
- If `value` does not exist, the toggle behaves as a pure boolean toggle.
- The client must ignore updates with a lower `version` than the current cached value.

### 6.5 Client Library Flows

#### 6.5.1 Pod Startup

1. The library reads local configuration.
2. The library registers the instance with the server.
3. The server returns the current snapshot.
4. The library initializes the local cache.

#### 6.5.2 Toggle Evaluation

- LOCAL_CACHE:
  - Query the local cache.

- REMOTE_ALWAYS:
  - Query the server in real time.

#### 6.5.3 Pod Shutdown

- The client calls DELETE /clients/register/{instanceId}.
- If it does not happen, the server expires the registration by heartbeat timeout.

### 6.6 Client Library Observability

- Log update application events (toggleName, version, result).
- Expose cache hit/miss metrics for LOCAL_CACHE.

---

## 7. Shared Rules (Server + Client Library)

### 7.1 Version Consistency

- Every toggle change increments `version` monotonically.
- The server sends `version` in bootstrap and callbacks.
- The client processes only versions greater than the local version.

### 7.2 Optional Value Contract

- `value` is optional as a child object.
- When present, `value.type` must be STRING|NUMBER and `value.raw` is required.
- When absent, the toggle behaves as a boolean toggle.

---

## 8. MVP Implementation Plan

### 8.1 Server Tracks

1. Database and migrations
- Create the main tables and indexes.

2. Toggle and client APIs
- Implement toggle, registration, heartbeat, and deregister endpoints.

3. Distribution pipeline
- Implement a PUT delivery worker with retry.

4. Observability
- Logs, metrics, and tracing.

### 8.2 Client Library Tracks

1. Spring configuration
- Read `consumed` and `callback-url`.

2. Instance registration
- Build the full `callbackUrl` and register during startup.

3. Runtime evaluation
- Implement LOCAL_CACHE and REMOTE_ALWAYS.

4. Internal callback
- Expose the PUT endpoint and update the local cache with version control.

### 8.3 Kubernetes Integration

- Configure server-to-pod connectivity.
- Configure startup/shutdown hooks for register/deregister.
- Validate heartbeat expiration for dead pods.

---

## 9. Acceptance Criteria

- Pod registration works per instance.
- Startup snapshot is received.
- Update PUT is delivered to every registered instance.
- Per-instance synchronization is visible (SYNCED/OUT_OF_SYNC).
- LOCAL_CACHE and REMOTE_ALWAYS strategies work per toggle.
- Optional typed values (STRING/NUMBER) work in bootstrap and update flows.

---

## 10. Risks And Mitigations

- Risk: callbackUrl is unreachable in Kubernetes.
  - Mitigation: standardize pod discovery/addressing and validate during registration.

- Risk: stale records for dead pods.
  - Mitigation: heartbeat + timeout expiration.

- Risk: overwrite with an old version.
  - Mitigation: version validation in the client.

- Risk: high update rate creates heavy fan-out.
  - Mitigation: worker pool and concurrency control.

---

## 11. Future Evolution (Post-MVP)

- Server-side strategy governance (policy override).
- Rollout segmentation.
- Additional value types (BOOLEAN/JSON).
- Messaging for large-scale distribution.
- Administrative operations dashboard.
