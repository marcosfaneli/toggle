package com.toggle.server.toggle.web;

import com.toggle.server.toggle.domain.Toggle;

import java.time.LocalDateTime;

public record ToggleResponse(
        String id,
        String name,
        String ownerServiceName,
        boolean enabled,
        long version,
        LocalDateTime updatedAt) {

    public static ToggleResponse from(Toggle toggle) {
        return new ToggleResponse(
                toggle.publicId(),
                toggle.name(),
                toggle.ownerServiceName(),
                toggle.enabled(),
                toggle.version(),
                toggle.updatedAt());
    }
}
