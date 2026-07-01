package com.toggle.server.toggle.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.toggle.server.shared.web.ToggleValueDto;
import com.toggle.server.toggle.domain.Toggle;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ToggleResponse(
        String id,
        String name,
        String maintainer,
        boolean enabled,
        long version,
    Instant updatedAt,
        ToggleValueDto value) {

    public static ToggleResponse from(Toggle toggle) {
        return new ToggleResponse(
                toggle.publicId(),
                toggle.name(),
                toggle.maintainer(),
                toggle.enabled(),
                toggle.version(),
                toggle.updatedAt(),
                ToggleValueDto.from(toggle.value()));
    }
}
