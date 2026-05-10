package com.toggle.server.delivery.domain;

public record ToggleUpdatedEvent(
        String toggleName,
        long targetVersion) {
}
