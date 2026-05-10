package com.toggle.client.runtime;

import java.time.Instant;

public record ToggleSnapshot(
        String name,
        boolean enabled,
        ToggleValue value,
        long version,
        Instant updatedAt) {
}
