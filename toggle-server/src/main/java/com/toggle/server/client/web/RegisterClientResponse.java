package com.toggle.server.client.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.toggle.server.client.application.RegisterClientResult;
import com.toggle.server.shared.web.ToggleValueDto;
import com.toggle.server.toggle.domain.Toggle;

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
            ToggleValueDto value,
            long version,
            Instant updatedAt) {

        static ToggleEntry from(Toggle toggle) {
            return new ToggleEntry(
                    toggle.name(),
                    toggle.enabled(),
                    ToggleValueDto.from(toggle.value()),
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
