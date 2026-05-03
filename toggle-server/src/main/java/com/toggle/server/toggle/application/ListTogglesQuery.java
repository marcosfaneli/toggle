package com.toggle.server.toggle.application;

public record ListTogglesQuery(
        String ownerServiceName,
        Boolean enabled) {
}
