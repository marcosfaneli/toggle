package com.toggle.server.delivery.domain;

public record ToggleUpdatedEvent(
        String toggleName,
        String ownerServiceName,
        long targetVersion) {
}
