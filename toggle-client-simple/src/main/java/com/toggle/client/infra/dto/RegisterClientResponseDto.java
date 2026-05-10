package com.toggle.client.infra.dto;

import java.time.Instant;
import java.util.List;

public record RegisterClientResponseDto(
        String serviceName,
        String instanceId,
        List<ToggleEntryDto> toggles) {

    public record ToggleEntryDto(
            String name,
            boolean enabled,
            ValueDto value,
            long version,
            Instant updatedAt) {
    }

    public record ValueDto(String type, String raw) {
    }
}
