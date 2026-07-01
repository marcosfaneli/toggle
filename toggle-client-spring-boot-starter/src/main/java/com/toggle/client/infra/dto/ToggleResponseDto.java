package com.toggle.client.infra.dto;

import java.time.Instant;

public record ToggleResponseDto(
        String id,
        String name,
        String maintainer,
        boolean enabled,
        long version,
        Instant updatedAt,
        ValueResponseDto value) {

    public record ValueResponseDto(String type, String raw) {
    }
}
