package com.toggle.client.runtime;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToggleCacheTest {

    @Test
    void shouldIgnoreOlderVersion() {
        var cache = new ToggleCache();
        var current = new ToggleSnapshot("checkout", true, new ToggleValue("STRING", "A"), 5, Instant.now());
        var older = new ToggleSnapshot("checkout", false, new ToggleValue("STRING", "B"), 4, Instant.now());

        cache.putIfNewer(current);
        cache.putIfNewer(older);

        var result = cache.get("checkout");
        assertTrue(result.isPresent());
        assertEquals(5, result.get().version());
        assertTrue(result.get().enabled());
        assertEquals("A", result.get().value().raw());
    }

    @Test
    void shouldAcceptNewerVersion() {
        var cache = new ToggleCache();
        var current = new ToggleSnapshot("checkout", true, null, 2, Instant.now());
        var newer = new ToggleSnapshot("checkout", false, null, 3, Instant.now());

        cache.putIfNewer(current);
        cache.putIfNewer(newer);

        var result = cache.get("checkout");
        assertTrue(result.isPresent());
        assertEquals(3, result.get().version());
        assertTrue(!result.get().enabled());
    }
}
