# Toggle Client Spring Boot Starter

Spring Boot starter for consuming Switchboard feature toggles.

## Dependency

```xml
<dependency>
    <groupId>com.toggle</groupId>
    <artifactId>toggle-client-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Configuration

```yaml
feature:
  toggles:
    service-name: checkout-service
    instance-id: checkout-local-1
    pod-name: checkout-local-1
    namespace: local
    server-base-url: http://localhost:8080
    callback-base-url: http://localhost:8082
    callback-path: /internal/feature-toggles
    heartbeat-interval-ms: 5000
    consumed:
      novo-checkout: LOCAL_CACHE
      pagamento-v2: REMOTE_ALWAYS
```

## Usage

```java
import com.toggle.client.runtime.FeatureToggleClient;

class CheckoutService {

    private final FeatureToggleClient featureToggleClient;

    CheckoutService(FeatureToggleClient featureToggleClient) {
        this.featureToggleClient = featureToggleClient;
    }

    void checkout() {
        if (featureToggleClient.isEnabled("pagamento-v2")) {
            // new flow
        }

        featureToggleClient.getValue("pagamento-v2")
                .ifPresent(value -> {
                    // value.type() and value.raw()
                });
    }
}
```

The starter always registers the client with the server and exposes the callback:

```http
PUT /internal/feature-toggles/{toggleName}
```
