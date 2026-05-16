# Switchboard

![Switchboard](./switchboard-icon.svg)

Feature Toggle Server - manage boolean feature flags across your microservices.

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

## Importing the API collection

Import `insomnia-collection.json` into Insomnia to test the endpoints.

.
