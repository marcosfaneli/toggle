package com.toggle.server.delivery.infrastructure;

public record ToggleCallbackPayload(
        String name,
        String maintainer,
        boolean enabled,
        long version,
        ValuePayload value) {

    public record ValuePayload(String type, String raw) {}
}
