# Toggle Client With Lib

Example Spring Boot application consuming `toggle-client-spring-boot-starter`.

The application itself only defines business endpoints. Registration, heartbeat,
server communication, local cache and callback handling are provided by the
starter.

## Run

Start the toggle server first:

```bash
docker compose up mysql toggle-server
```

Then, from the repository root, run:

```bash
mvn -pl toggle-client-with-lib -am spring-boot:run
```

The application runs on `http://localhost:8083`.

Running from the repository root with `-pl toggle-client-with-lib -am` makes
Maven build the local `toggle-client-spring-boot-starter` dependency together
with the example application.

## Try

```bash
curl http://localhost:8083/checkout/toggles/payment-v2/decision
```

The starter callback remains available at:

```http
PUT /internal/feature-toggles/{toggleName}
```
