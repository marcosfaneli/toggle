package com.toggle.server.toggle.application;

public record CreateToggleCommand(
        String name,
        String ownerServiceName,
        boolean enabled) {
}
