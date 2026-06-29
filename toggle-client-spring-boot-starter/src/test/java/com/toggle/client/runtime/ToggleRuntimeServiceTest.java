package com.toggle.client.runtime;

import com.toggle.client.config.FeatureToggleProperties;
import com.toggle.client.infra.ToggleServerClient;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ToggleRuntimeServiceTest {

    @Test
    void shouldResolveFromCacheWhenLocalCacheMode() {
        var cache = new ToggleCache();
        cache.putIfNewer(new ToggleSnapshot("new-checkout", true, null, 1, Instant.now()));

        var serverClient = new StubServerClient(Optional.empty());
        var service = new ToggleRuntimeService(properties(), cache, serverClient);

        var resolution = service.resolve("new-checkout");

        assertThat(resolution.found()).isTrue();
        assertThat(resolution.enabled()).isTrue();
        assertThat(service.isEnabled("new-checkout")).isTrue();
        assertThat(resolution.mode()).isEqualTo(ConsumeMode.LOCAL_CACHE);
        assertThat(resolution.source()).isEqualTo("CACHE");
    }

    @Test
    void shouldResolveValueFromRemoteWhenRemoteAlwaysMode() {
        var cache = new ToggleCache();
        var remote = new ToggleSnapshot("payment-v2", false, new ToggleValue("NUMBER", "10"), 4, Instant.now());

        var serverClient = new StubServerClient(Optional.of(remote));
        var service = new ToggleRuntimeService(properties(), cache, serverClient);

        var resolution = service.resolve("payment-v2");

        assertThat(resolution.found()).isTrue();
        assertThat(resolution.enabled()).isFalse();
        assertThat(service.getValue("payment-v2"))
                .hasValue(new ToggleValue("NUMBER", "10"));
        assertThat(resolution.mode()).isEqualTo(ConsumeMode.REMOTE_ALWAYS);
        assertThat(resolution.source()).isEqualTo("REMOTE");
        assertThat(cache.get("payment-v2")).isPresent();
        assertThat(cache.get("payment-v2").get().version()).isEqualTo(4);
    }

    private FeatureToggleProperties properties() {
        return new FeatureToggleProperties(
                true,
                "checkout-service",
                "instance-1",
                "pod-1",
                "default",
                "http://localhost:8080",
                "http://localhost:8082",
                "/internal/feature-toggles",
                30_000,
                Map.of(
                        "new-checkout", ConsumeMode.LOCAL_CACHE,
                        "payment-v2", ConsumeMode.REMOTE_ALWAYS));
    }

    private static final class StubServerClient implements ToggleServerClient {

        private final Optional<ToggleSnapshot> remoteValue;

        private StubServerClient(Optional<ToggleSnapshot> remoteValue) {
            this.remoteValue = remoteValue;
        }

        @Override
        public List<ToggleSnapshot> registerAndGetSnapshot() {
            return List.of();
        }

        @Override
        public boolean isRegistered() {
            return true;
        }

        @Override
        public void heartbeat() {
        }

        @Override
        public void deregister() {
        }

        @Override
        public Optional<ToggleSnapshot> fetchToggleByName(String toggleName) {
            return remoteValue.filter(snapshot -> snapshot.name().equals(toggleName));
        }
    }
}
