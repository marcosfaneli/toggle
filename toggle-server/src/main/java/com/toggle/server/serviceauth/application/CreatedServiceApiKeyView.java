package com.toggle.server.serviceauth.application;

import java.time.Instant;

public record CreatedServiceApiKeyView(
        String id,
        String serviceName,
        String name,
        String apiKey,
        Instant createdAt) {
}
