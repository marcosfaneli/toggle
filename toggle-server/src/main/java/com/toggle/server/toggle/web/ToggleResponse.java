package com.toggle.server.toggle.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.toggle.server.toggle.domain.Toggle;
import com.toggle.server.toggle.domain.ToggleValue;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ToggleResponse(
        String id,
        String name,
        String maintainer,
        boolean enabled,
        long version,
    Instant updatedAt,
        ValueResponse value) {

    public record ValueResponse(String type, String raw) {
        static ValueResponse from(ToggleValue toggleValue) {
            if (toggleValue == null) return null;
            return new ValueResponse(toggleValue.type().name(), toggleValue.raw());
        }
    }

    public static ToggleResponse from(Toggle toggle) {
        return new ToggleResponse(
                toggle.publicId(),
                toggle.name(),
                toggle.maintainer(),
                toggle.enabled(),
                toggle.version(),
                toggle.updatedAt(),
                ValueResponse.from(toggle.value()));
    }
}
