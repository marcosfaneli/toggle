package com.toggle.server.serviceauth.application;

import java.time.Instant;

public record ServiceApiKeyView(
        String id,
        String serviceName,
        String name,
        String status,
        Instant createdAt,
        Instant lastUsedAt,
        Instant revokedAt) {
}
