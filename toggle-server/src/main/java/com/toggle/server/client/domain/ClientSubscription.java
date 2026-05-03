package com.toggle.server.client.domain;

import java.time.Instant;

public record ClientSubscription(
        String publicId,
        Long clientInstanceId,
        String toggleName,
        ConsumeMode consumeMode,
        Instant createdAt) {
}
