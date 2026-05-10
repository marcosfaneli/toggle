package com.toggle.client.runtime;

import com.toggle.client.config.FeatureToggleProperties;
import com.toggle.client.infra.ToggleServerClient;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToggleRuntimeServiceTest {

    @Test
    void shouldResolveFromCacheWhenLocalCacheMode() {
        var cache = new ToggleCache();
        cache.putIfNewer(new ToggleSnapshot("novo-checkout", true, null, 1, Instant.now()));

        var serverClient = new StubServerClient(Optional.empty());
        var service = new ToggleRuntimeService(properties(), cache, serverClient);

        var resolution = service.resolve("novo-checkout");

        assertTrue(resolution.found());
        assertTrue(resolution.enabled());
        assertEquals(ConsumeMode.LOCAL_CACHE, resolution.mode());
        assertEquals("CACHE", resolution.source());
    }

    @Test
    void shouldResolveFromRemoteWhenRemoteAlwaysMode() {
        var cache = new ToggleCache();
        var remote = new ToggleSnapshot("pagamento-v2", false, new ToggleValue("NUMBER", "10"), 4, Instant.now());

        var serverClient = new StubServerClient(Optional.of(remote));
        var service = new ToggleRuntimeService(properties(), cache, serverClient);

        var resolution = service.resolve("pagamento-v2");

        assertTrue(resolution.found());
        assertFalse(resolution.enabled());
        assertEquals(ConsumeMode.REMOTE_ALWAYS, resolution.mode());
        assertEquals("REMOTE", resolution.source());
        assertTrue(cache.get("pagamento-v2").isPresent());
        assertEquals(4, cache.get("pagamento-v2").get().version());
    }

    private FeatureToggleProperties properties() {
        return new FeatureToggleProperties(
                "checkout-service",
                "instance-1",
                "pod-1",
                "default",
                "http://localhost:8080",
                "http://localhost:8082",
                "/internal/feature-toggles",
                30_000,
                Map.of(
                        "novo-checkout", ConsumeMode.LOCAL_CACHE,
                        "pagamento-v2", ConsumeMode.REMOTE_ALWAYS));
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
