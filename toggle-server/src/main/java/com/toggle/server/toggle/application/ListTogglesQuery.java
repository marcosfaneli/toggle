package com.toggle.server.toggle.application;

import org.springframework.lang.Nullable;

public record ListTogglesQuery(
        @Nullable String ownerServiceName,
        Boolean enabled) {
}
