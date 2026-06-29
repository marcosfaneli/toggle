package com.toggle.client.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ToggleCache {

    private static final Logger log = LoggerFactory.getLogger(ToggleCache.class);

    private final ConcurrentHashMap<String, ToggleSnapshot> cache = new ConcurrentHashMap<>();

    public Optional<ToggleSnapshot> get(String toggleName) {
        return Optional.ofNullable(cache.get(toggleName));
    }

    public void putIfNewer(ToggleSnapshot incoming) {
        cache.compute(incoming.name(), (name, current) -> {
            if (current != null && incoming.version() < current.version()) {
                log.info("Ignored stale update toggle={} incomingVersion={} currentVersion={}",
                        name, incoming.version(), current.version());
                return current;
            }
            return incoming;
        });
    }
}
