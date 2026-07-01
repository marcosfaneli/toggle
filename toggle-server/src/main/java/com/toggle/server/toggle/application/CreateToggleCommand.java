package com.toggle.server.toggle.application;

public record CreateToggleCommand(
        String name,
        String maintainer,
        boolean enabled,
        ToggleValueCommand value) {

    public record ToggleValueCommand(String type, String raw) {}
}
