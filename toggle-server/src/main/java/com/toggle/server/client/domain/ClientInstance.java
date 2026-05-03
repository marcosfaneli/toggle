package com.toggle.server.client.domain;

import java.time.LocalDateTime;

public record ClientInstance(
        String publicId,
        String serviceName,
        String instanceId,
        String podName,
        String namespace,
        String callbackUrl,
        ClientInstanceStatus status,
        LocalDateTime registeredAt) {
}
