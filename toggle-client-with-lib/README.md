# Toggle Client With Lib

Example Spring Boot application consuming `toggle-client-spring-boot-starter`.

The application itself only defines business endpoints. Registration, heartbeat,
server communication, local cache and callback handling are provided by the
starter.

## Run

Start the toggle server first, then run:

```bash
mvn spring-boot:run
```

The application runs on `http://localhost:8083`.

## Try

```bash
curl http://localhost:8083/checkout/toggles/payment-v2/decision
```

The starter callback remains available at:

```http
PUT /internal/feature-toggles/{toggleName}
```
