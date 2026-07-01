package com.toggle.server.toggle.application;

public record UpdateToggleCommand(
        String name,
        String maintainer,
        Boolean enabled,
        ValueUpdate valueUpdate) {

    public sealed interface ValueUpdate {
        record Keep()                       implements ValueUpdate {}
        record Remove()                     implements ValueUpdate {}
        record Set(String type, String raw) implements ValueUpdate {}
    }
}
