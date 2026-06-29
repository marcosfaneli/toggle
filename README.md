# Switchboard

![Switchboard](./switchboard-icon.svg)

Switchboard is a feature toggle platform for Spring-based microservices. It
provides a central server for creating and updating feature flags, plus client
applications and a reusable Spring Boot starter that register service instances,
keep local toggle state in sync, and receive updates through HTTP callbacks.

The project is currently under active development. The core MVP covers boolean
toggles, optional typed values, client registration, heartbeat, local-cache
updates, and Minikube validation, but the API and operational model may still
change as the platform evolves.

## Stack

- Java 25 · Spring Boot 3.5.14 · Undertow
- MySQL 8.4 · Flyway · Spring Data JPA
- ULID public IDs · RFC 7807 error responses

## Running locally

**Start the database:**

```bash
docker compose up -d
```

**Start the server:**

```bash
cd toggle-server
mvn spring-boot:run
```

Server runs on `http://localhost:8080`.  
Adminer (DB admin) on `http://localhost:8081`.

The server reads the database connection from environment variables, with local defaults:

| Variable | Default |
|----------|---------|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `3306` |
| `DB_NAME` | `toggle_db` |
| `DB_USERNAME` | `toggle` |
| `DB_PASSWORD` | `toggle` |

When running the server container on the same Docker Compose network as MySQL, set `DB_HOST=mysql`.

## API

### Create a toggle

```http
POST /toggles
Content-Type: application/json

{
  "name": "my-feature",
  "ownerServiceName": "order-service",
  "enabled": true,
  "value": {
    "type": "STRING",
    "raw": "variant-a"
  }
}
```

`value` is optional. When provided, `type` must be `STRING` or `NUMBER`.

**201 Created**

```json
{
  "id": "01JPXYZ...",
  "name": "my-feature",
  "ownerServiceName": "order-service",
  "enabled": true,
  "version": 1,
  "updatedAt": "2026-04-21T17:00:00",
  "value": {
    "type": "STRING",
    "raw": "variant-a"
  }
}
```

`value` is omitted from the response when the toggle has no value.

### Update a toggle

```http
PATCH /toggles/{name}
Content-Type: application/json

{
  "ownerServiceName": "order-service",
  "enabled": false,
  "value": {
    "type": "NUMBER",
    "raw": "10"
  }
}
```

`ownerServiceName` belongs to the request body (not query params).

- Omit `value` to keep the current value unchanged.
- Send `"value": null` to remove the current value.
- `ownerServiceName` can be omitted when the toggle name is unique across services.

**200 OK**

Returns the updated toggle.

**Errors** follow [RFC 7807](https://datatracker.ietf.org/doc/html/rfc7807) (`application/problem+json`):

| Status | Cause |
|--------|-------|
| 400 | Missing or blank required fields, or invalid `value.type` |
| 409 | Toggle already exists for that name + service |

## Testing

```bash
cd toggle-server
mvn test
```

## Feature Toggle Client Library

The reusable Spring Boot client is available in
[`toggle-client-spring-boot-starter`](./toggle-client-spring-boot-starter).
It handles registration, heartbeat, server communication, local cache and the
callback endpoint:

```http
PUT /internal/feature-toggles/{toggleName}
```

Applications consume it by adding the dependency and injecting
`FeatureToggleClient`:

```java
if (featureToggleClient.isEnabled("payment-v2")) {
    // new flow
}

featureToggleClient.getValue("payment-v2")
        .ifPresent(value -> {
            // value.type() and value.raw()
        });
```

[`toggle-client-with-lib`](./toggle-client-with-lib) is an example application
using the starter. The original [`toggle-client-simple`](./toggle-client-simple)
remains intact as the historical/manual client implementation.

## Minikube

Use the manifests and walkthrough in [`k8s/minikube`](./k8s/minikube/README.md)
to validate the server and multiple client replicas in a local Kubernetes
cluster.

## Documentation

- [MVP technical specification](./doc/feature-toggle-mvp-technical-specification.md)
- [MVP implementation stories](./doc/mvp-implementation-stories.md)
- [Feature toggle flow](./doc/feature-toggle-flow.puml)
- [Data model](./doc/feature-toggle-data-model.puml)

## Importing the API collection

Import `insomnia-collection.json` into Insomnia to test the endpoints.
