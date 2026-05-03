package com.toggle.server.client.domain;

import java.time.LocalDateTime;

public record ClientSubscription(
        String publicId,
        Long clientInstanceId,
        String toggleName,
        ConsumeMode consumeMode,
        LocalDateTime createdAt) {
}
