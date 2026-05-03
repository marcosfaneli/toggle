package com.toggle.server.toggle.domain;

import java.time.Instant;

public record Toggle(
        String publicId,
        String name,
        String ownerServiceName,
        boolean enabled,
        long version,
        Instant updatedAt,
        ToggleValue value) {
}
