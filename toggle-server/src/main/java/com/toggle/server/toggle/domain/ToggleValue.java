package com.toggle.server.toggle.domain;

public record ToggleValue(
        ValueType type,
        String raw) {
}
