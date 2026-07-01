package com.toggle.server.toggle.domain;

import java.time.Instant;

public record Toggle(
        String publicId,
        String name,
        String maintainer,
        boolean enabled,
        long version,
        Instant updatedAt,
        ToggleValue value) {
}
