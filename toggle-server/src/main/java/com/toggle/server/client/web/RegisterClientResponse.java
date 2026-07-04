package com.toggle.server.client.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.toggle.server.client.application.RegisterClientResult;
import com.toggle.server.toggle.domain.Toggle;
import com.toggle.server.toggle.domain.ToggleValue;

import java.time.Instant;
import java.util.List;

public record RegisterClientResponse(
        String serviceName,
        String instanceId,
        List<ToggleEntry> toggles) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToggleEntry(
            String name,
            boolean enabled,
            ValueEntry value,
            long version,
            Instant updatedAt) {

        public record ValueEntry(String type, String raw) {
            static ValueEntry from(ToggleValue toggleValue) {
                if (toggleValue == null) return null;
                return new ValueEntry(toggleValue.type().name(), toggleValue.raw());
            }
        }

        static ToggleEntry from(Toggle toggle) {
            return new ToggleEntry(
                    toggle.name(),
                    toggle.enabled(),
                    ValueEntry.from(toggle.value()),
                    toggle.version(),
                    toggle.updatedAt());
        }
    }

    public static RegisterClientResponse from(RegisterClientResult result) {
        return new RegisterClientResponse(
                result.instance().serviceName(),
                result.instance().instanceId(),
                result.toggleSnapshot().stream().map(ToggleEntry::from).toList());
    }
}
