package com.toggle.server.delivery.infrastructure;

public record ToggleCallbackPayload(
        String name,
        String ownerServiceName,
        boolean enabled,
        long version,
        ValuePayload value) {

    public record ValuePayload(String type, String raw) {}
}
