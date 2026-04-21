package com.toggle.server.toggle.domain;

import java.time.LocalDateTime;

public record Toggle(
        String publicId,
        String name,
        String ownerServiceName,
        boolean enabled,
        long version,
        LocalDateTime updatedAt,
        ToggleValue value) {
}
