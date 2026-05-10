package com.toggle.server.toggle.application;

public record UpdateToggleCommand(
        String name,
        Boolean enabled,
        ValueUpdate valueUpdate) {

    public sealed interface ValueUpdate {
        record Keep()                       implements ValueUpdate {}
        record Remove()                     implements ValueUpdate {}
        record Set(String type, String raw) implements ValueUpdate {}
    }
}
