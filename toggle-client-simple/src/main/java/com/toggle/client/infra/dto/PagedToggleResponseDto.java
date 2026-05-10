package com.toggle.client.infra.dto;

import java.time.Instant;
import java.util.List;

public record PagedToggleResponseDto(
        List<ToggleResponseDto> content,
        int number,
        int size,
        boolean first,
        boolean last) {

    public record ToggleResponseDto(
            String id,
            String name,
            String ownerServiceName,
            boolean enabled,
            long version,
            Instant updatedAt,
            ValueDto value) {
    }

    public record ValueDto(String type, String raw) {
    }
}
